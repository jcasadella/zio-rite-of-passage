package com.rockthejvm.reviewboard.pages

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.http.requests.RecoverPasswordRequest

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html.Element

case class RecoverPasswordState(
    email: String = "",
    token: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override def errorList: List[Option[String]] = List(
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid"),
    Option.when(token.isEmpty())("Token can't be empty"),
    Option.when(newPassword.isEmpty())("Pasword can't be empty"),
    Option.when(newPassword != confirmPassword)("Passwords must match")
  ) ++ upstreamStatus.map(_.left.toOption).toList

  override def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)

}

object RecoverPasswordPage extends FormPage[RecoverPasswordState]("Recover Password") {

  override def basicState: RecoverPasswordState = RecoverPasswordState()

  val submitter = Observer[RecoverPasswordState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.recoverPasswordEndpoint(
          RecoverPasswordRequest(state.email, state.token, state.newPassword)
        )
      )
        .map { _ =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Password updated! You can log in now."))
            )
          )
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage()))))
          }
        }
        .runJs
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamStatus = None),
      maybeAutoComplete = Some("username")
    ),
    renderInput(
      "Recovery Token (from email)",
      "token-input",
      "text",
      true,
      "The Token",
      (s, t) => s.copy(token = t, showStatus = false, upstreamStatus = None)
    ),
    // an input of type password
    renderInput(
      "New Password",
      "password-input",
      "password",
      true,
      "Your new password",
      (s, p) => s.copy(newPassword = p, showStatus = false, upstreamStatus = None),
      maybeAutoComplete = Some("new-password")
    ),
    // an input of type password
    renderInput(
      "Confirm Password",
      "confirm-password-input",
      "password",
      true,
      "Confirm password",
      (s, p) => s.copy(confirmPassword = p, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Reset Password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    Anchors.renderNavLink(
      "Need a password recovery token?",
      "/forgot",
      "auth-link"
    )
  )

}
