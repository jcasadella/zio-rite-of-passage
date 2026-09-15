package com.rockthejvm.reviewboard.pages

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.http.requests.RegisterUserAccount

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html

case class SignUpFormState(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  private val emailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val passwordError: Option[String] =
    Option.when(password.isEmpty())("Password can't be empty")
  private val confirmPasswordError: Option[String] =
    Option.when(password != confirmPassword)("Passwords must match")

  override val errorList: List[Option[String]] =
    List(emailError, passwordError, confirmPasswordError) ++
      upstreamStatus.map(_.left.toOption).toList
  override val maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)
}

object SignUpPage extends FormPage[SignUpFormState]("Sign Up") {

  override def basicState: SignUpFormState = SignUpFormState()

  val submitter = Observer[SignUpFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.createUserEndpoint(RegisterUserAccount(state.email, state.password)))
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Account created! You can log in now."))
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

  override def renderChildren(): List[ReactiveHtmlElement[html.Element]] = List(
    // an input of type text
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamStatus = None)
    ),
    // an input of type password
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upstreamStatus = None)
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
      "Sign Up",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

}
