package com.rockthejvm.reviewboard.components

import zio.*

import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.*

class AddReviewCard(companyId: Long, onDisable: () => Unit, triggerBus: EventBus[Unit]) {

  case class ReviewState(
      review: Review = Review.empty(companyId),
      showErrors: Boolean = false,
      upstreamError: Option[String] = None
  )

  val stateVar = Var(ReviewState())

  val submitter = Observer[ReviewState] { state =>
    if (state.upstreamError.nonEmpty) {
      stateVar.update(_.copy(showErrors = true))
    } else {
      useBackend(_.reviews.createReviewEndpoint(CreateReviewRequest.fromReview(state.review)))
        .map { reviewResponse => onDisable() }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showErrors = true, upstreamError = Some(e.getMessage())))
          }
        }
        .emitTo(triggerBus)
    }
  }

  def apply() =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description add-review",
          div(
            // score dropdowns
            div(
              cls := "add-review-scores",
              renderDropDown("Would recommend?", (r, v) => r.copy(wouldRecommend = v)),
              renderDropDown("Management", (r, v) => r.copy(management = v)),
              renderDropDown("Culture", (r, v) => r.copy(culture = v)),
              renderDropDown("Salary", (r, v) => r.copy(salary = v)),
              renderDropDown("Benefits", (r, v) => r.copy(benefits = v))
            ),
            // text area for the text review
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here",
                onInput.mapToValue --> stateVar.updater { (s: ReviewState, v: String) =>
                  s.copy(review = s.review.copy(review = v))
                }
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
            ),
            a(
              cls  := "add-review-cancel",
              href := "#",
              onClick --> (_ => onDisable()),
              "Cancel"
            ),
            children <-- stateVar.signal
              .map(s => s.upstreamError.filter(_ => s.showErrors))
              .map(maybeRenderError)
              .map(_.toList)
          )
        )
      )
    )

  private def renderDropDown(name: String, updateFn: (Review, Int) => Review) = {
    val selectorId = name.split(" ").map(_.toLowerCase()).mkString("-")
    div(
      cls := "add-review-score",
      label(forId := selectorId, s"$name:"),
      select(
        idAttr := selectorId,
        (1 to 5).reverse.map { v =>
          option(v.toString)
        },
        onInput.mapToValue --> stateVar.updater { (s: ReviewState, value: String) =>
          {
            s.copy(review = updateFn(s.review, value.toInt))
          }
        }
      )
    )
  }

  private def maybeRenderError(maybeError: Option[String]) = maybeError.map { message =>
    div(
      cls := "page-status-errors",
      message
    )
  }
}
