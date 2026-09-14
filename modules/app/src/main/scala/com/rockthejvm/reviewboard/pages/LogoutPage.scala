package com.rockthejvm.reviewboard.pages

import com.rockthejvm.reviewboard.core.Session

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element

case class LogoutState() extends FormState {

  override def errorList: List[Option[String]] = List()

  override def maybeSuccess: Option[String] = None

  override def showStatus: Boolean = false

}

object LogoutPage extends FormPage[LogoutState]("Log Out") {
  override val stateVar: Var[LogoutState] = Var(LogoutState())

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "logout-status",
      "You have been successfully logged out."
    )
  )
}
