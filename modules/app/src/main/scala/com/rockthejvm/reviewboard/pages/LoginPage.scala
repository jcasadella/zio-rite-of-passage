package com.rockthejvm.reviewboard.pages

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.http.requests.LoginRequest

import com.raquo.laminar.api.L.{*, given}
import frontroute.BrowserNavigation
import org.scalajs.dom

case class LoginState(
    email: String = "",
    password: String = "",
    upstreamError: Option[String] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val emailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val passwordError: Option[String] =
    Option.when(password.isEmpty())("Password can't be empty")

  override val errorList = List(emailError, passwordError, upstreamError)

  override val maybeSuccess: Option[String] = None
}

object LoginPage extends FormPage[LoginState]("Log In") {

  override val stateVar: Var[LoginState] = Var(LoginState())

  val submitter = Observer[LoginState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.loginEndpoint(LoginRequest(state.email, state.password)))
        .map { userToken =>
          Session.setUserState(userToken)
          stateVar.set(LoginState())
          BrowserNavigation.replaceState("/")
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
          }
        }
        .runJs
    }
  }

  override def renderChildren() = List(
    // an input of type text
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamError = None)
    ),
    // an input of type password
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upstreamError = None)
    ),
    button(
      `type` := "button",
      "Log In",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )
}
