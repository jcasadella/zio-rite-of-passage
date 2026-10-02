package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.AddReviewCard
import com.rockthejvm.reviewboard.components.CompanyComponents
import com.rockthejvm.reviewboard.components.Markdown
import com.rockthejvm.reviewboard.components.Router
import com.rockthejvm.reviewboard.components.Time
import com.rockthejvm.reviewboard.core.Session
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.domain.data.ReviewSummary
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.requests.InvitePackRequest
import org.scalajs.dom
import zio.*

import java.time.Instant
import scala.scalajs.js.Date

object CompanyPage {

  enum PageStatus {
    case Loading
    case NotFound
    case Found(company: Company)
  }

  val addReviewCardActive = Var[Boolean](false)
  val fetchCompanyBus = EventBus[Option[Company]]()
  val triggerRefreshBus = EventBus[Unit]()
  val status = fetchCompanyBus.events.scanLeft(PageStatus.Loading)((s, maybeCompany) => maybeCompany match {
    case None => PageStatus.NotFound
    case Some(company) => PageStatus.Found(company)
  })

  val inviteErrorBus = EventBus[String]()

  def refreshReviewList(companyId: Long) =
    useBackend(_.reviews.getReviewsByCompanyIdEndpoint(companyId))
    .toEventStream
    .mergeWith(
      triggerRefreshBus.events
      .flatMap(_ =>
        useBackend(_.reviews.getReviewsByCompanyIdEndpoint(companyId))
        .toEventStream
      )
    )

  def reviewsSignal: Signal[List[Review]] = fetchCompanyBus.events.flatMap {
    case None => EventStream.empty
    case Some(company) => refreshReviewList(company.id)
      
  }.scanLeft(List[Review]())((_, list) => list)

  def render(company: Company) = List(
    div(
      cls := "row jvm-companies-details-top-card",
      div(
        cls := "col-md-12 p-0",
        div(
          cls := "jvm-companies-details-card-profile-img",
          CompanyComponents.renderCompanyPicture(company)
        ),
        div(
          cls := "jvm-companies-details-card-profile-title",
          h1(company.name),
          div(
            cls := "jvm-companies-details-card-profile-company-details-company-and-location",
            CompanyComponents.renderOverview(company)
          )
        ),
        child <-- Session.userState.signal.map(maybeUser => maybeRenderUserAction(maybeUser, reviewsSignal))
      )
    ),
    div(
      cls := "container-fluid",
      renderCompanySummary(company),
      children <-- addReviewCardActive.signal
        .map(active => Option.when(active)(
          AddReviewCard(
            company.id,
            () => addReviewCardActive.set(false),
            triggerRefreshBus
          )()
          ))
        .map(_.toList),
      children <-- reviewsSignal.map(_.map(renderReview)),
      child.maybe <-- Session.userState.signal.map(_.map(_ => renderInviteAction(company)))
    )
  )

  def renderInviteAction(company: Company) =
    div(
        cls := "container",
        div(
          cls := "rok-last",
          div(
            cls := "row invite-row",
            div(
              cls := "col-md-6 col-sm-6 col-6",
              span(
                cls := "rock-apply",
                p("Do you represent this company?"),
                p("Invite people to leave reviews.")
              )
            ),
            div(
              cls := "col-md-6 col-sm-6 col-6",
              button(
                `type` := "button",
                cls := "rock-action-btn",
                "Invite people",
                disabled <-- inviteErrorBus.events.mapTo(true).startWith(false),
                onClick.mapToUnit --> (_ => startPaymentFlow(company.id))
              ),
              div(
                child.text <-- inviteErrorBus.events
              )
            )
          )
        )
      )
  
  def startPaymentFlow(companyId: Long) =
    useBackend(_.invites.addPackPromotedEndpoint(InvitePackRequest(companyId)))
      .tapError(e => ZIO.succeed(inviteErrorBus.emit(e.getMessage())))
      .emitTo(Router.externalUrlBus)

  def maybeRenderUserAction(maybeUser: Option[UserToken], reviewsSignal: Signal[List[Review]]) =
    maybeUser match {
      case None =>
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          "You must be logged in to post a review"
        )
      case Some(user) =>
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          child <-- reviewsSignal.map(_.find(_.userId == user.id)).map {
            case None => 
              button(
                `type` := "button",
                cls    := "btn btn-warning",
                "Add a review",
                disabled <-- addReviewCardActive.signal,
                onClick.mapTo(true) --> addReviewCardActive.writer
              )
            case Some(_) => div("You already posted a review")
          }
       )
    }

  def renderCompanySummary(company: Company) = {
    val summaryBus = EventBus[Option[ReviewSummary]]()
    val buttonStatus = EventBus[Option[String]]()

    val getCurrentSummary =
      useBackend(_.reviews.getSummaryEndpoint(company.id))
    
    val refresher = Observer[Unit] { _ =>
      val program = for {
        _ <- ZIO.attempt(buttonStatus.emit(Some("Loading")))
        newSummary <- useBackend(_.reviews.makeSummaryEndpoint(company.id))
        _ <- ZIO.attempt(buttonStatus.emit(None))
      } yield newSummary
      
      program.emitTo(summaryBus)
    }

    div(
      onMountCallback(_ => getCurrentSummary.emitTo(summaryBus)),
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        h3(span("Review Summary")),
        div(
          cls := "company-description review-summary-contents",
          child <-- summaryBus.events.map(_.map(_.contents).getOrElse("No generated review yet")),
        ),
        child.maybe <-- summaryBus.events.map(
            _.map(_.created)
            .map(t => s"Generated by GPT-6 ${Time.unix2hr(t.toEpochMilli())}")
            .map(text => div(cls := "review-posted", text))
          ),
        button(
          `type` := "button",
          cls := "rock-action-btn generate-btn",
          disabled <-- summaryBus.events
            .map(
              _.map(summary => Time.past(summary.created.toEpochMilli()))
                .map(diff => diff < - 2 * 3600 * 1000) // 24 * 3600 * 1000)
                .getOrElse(false)
            )
            .mergeWith(buttonStatus.events.mapTo(true))
            .startWith(false),
          onClick.mapToUnit --> refresher,
          child.text <-- buttonStatus.events
            .map(_.getOrElse("Generate Summary"))
            .startWith("Generate Summary")
        )
      )
    )
  }

  def renderReview(review: Review) =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        cls.toggle("review-highlighted") <-- Session.userState.signal.map(_.exists(_.id == review.userId)),
        div(
          cls := "company-description",
          div(
            cls := "review-summary",
            renderReviewDetail("Would Recommend", review.wouldRecommend),
            renderReviewDetail("Management", review.management),
            renderReviewDetail("Culture", review.culture),
            renderReviewDetail("Salary", review.salary),
            renderReviewDetail("Benefits", review.benefits)
          ),
          injectMarkdown(review),
          div(cls := "review-posted", s"Posted ${Time.unix2hr(review.created.toEpochMilli())}"),
          child.maybe <-- Session.userState.signal
            .map(_.filter(_.id == review.userId))
            .map(_.map(_ => div(cls := "review-posted", "Your Review")))
        )
      )
    )

  def renderReviewDetail(detail: String, score: Int) =
    div(
      cls := "review-detail",
      span(cls := "review-detail-name", s"$detail: "),
      (1 to score).toList.map(_ =>
        svg.svg(
          svg.cls     := "review-rating",
          svg.viewBox := "0 0 32 32",
          svg.path(
            svg.d := "m15.1 1.58-4.13 8.88-9.86 1.27a1 1 0 0 0-.54 1.74l7.3 6.57-1.97 9.85a1 1 0 0 0 1.48 1.06l8.62-5 8.63 5a1 1 0 0 0 1.48-1.06l-1.97-9.85 7.3-6.57a1 1 0 0 0-.55-1.73l-9.86-1.28-4.12-8.88a1 1 0 0 0-1.82 0z"
          )
        )
      )
    )

  def apply(companySlug: String) =
    div(
      cls := "container-fluid the-rock",
      onMountCallback(_ => useBackend(_.companies.getByIdEndpoint(companySlug)).emitTo(fetchCompanyBus)),
      children <-- status.map {
        case PageStatus.Loading => renderLoading()
        case PageStatus.NotFound => renderCompanyNotFound()
        case PageStatus.Found(company) => render(company)
      }
    )

  def renderLoading() = List(
    div(
      cls := "simple-titled-page",
      h1("Loading...")
    )
  )

  def renderCompanyNotFound() = List(
    div(
      cls := "simple-titled-page",
      h1("Oops!"),
      h2("This comapny doesn't exist."),
      div(
        "Maybe check the ",
        a(
          href:="/",
          "list of companies "
        ),
        "again?"
      )
    )
  )
  
  def injectMarkdown(review: Review) =
    div(
      cls:= "review-content",
      DomApi
        .unsafeParseHtmlStringIntoNodeArray(Markdown.toHtml(review.review))
        .map {
          case t: dom.Text => span(t.data)
          case e: dom.html.Element => foreignHtmlElement(e)
          case _ => emptyNode
        }
    )
}
