package com.rockthejvm.reviewboard.components

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.domain.data.InviteNamedRecord
import com.rockthejvm.reviewboard.http.requests.InviteRequest
import com.rockthejvm.reviewboard.pages.CompanyPage.refreshReviewList

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object InviteActions {

  val inviteListBus = EventBus[List[InviteNamedRecord]]()

  def refreshInviteList() =
    useBackend(_.invites.getByUserIdEndpoint(()))

  def apply() =
    div(
      onMountCallback(_ => refreshInviteList().emitTo(inviteListBus)),
      cls := "profile-section",
      h3(span("Invites")),
      children <-- inviteListBus.events.map(_.sortBy(_.companyName)).map(_.map(renderInviteSection))
    )

  def renderInviteSection(record: InviteNamedRecord) = {
    val emailListVar  = Var[Array[String]](Array())
    val maybeErrorVar = Var[Option[String]](None)

    val inviteSubmitter = Observer[Unit] { _ =>
      val emailList = emailListVar.now().toList
      if (emailList.exists(!_.matches(Constants.emailRegex)))
        maybeErrorVar.set(Some("At least an email is invalid"))
      else {
        val refreshProgram = for {
          _ <- useBackend(_.invites.inviteEndpoint(InviteRequest(record.companyId, emailList)))
          invitesLeft <- refreshInviteList()
        } yield invitesLeft

        maybeErrorVar.set(None)
        refreshProgram.emitTo(inviteListBus)
      }
    }

    div(
      cls := "invite-section",
      h5(span(record.companyName)),
      p(s"${record.nInvites} invites left"),
      textArea(
        cls         := "invites-area",
        placeholder := "Enter emails, one per line",
        onInput.mapToValue.map(_.split("\n").map(_.trim).filter(_.nonEmpty)) --> emailListVar.writer
      ),
      button(
        `type` := "button",
        cls    := "btn btn-primary",
        "Invite",
        onClick.mapToUnit --> inviteSubmitter
      ),
      child.maybe <-- maybeErrorVar.signal.map(maybeRenderError)
    )
  }

  private def maybeRenderError(maybeError: Option[String]) = maybeError.map { message =>
    div(
      cls := "page-status-errors",
      message
    )
  }
}
