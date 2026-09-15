package com.rockthejvm.reviewboard.components

import com.rockthejvm.reviewboard.pages.*

import com.raquo.laminar.api.L.{*, given}
import frontroute.*
import org.scalajs.dom

object Router {
  def apply() =
    mainTag( // <main>
      routes(
        div(
          cls := "container-fluid",
          (pathEnd | path("companies")) { // localhost:1234 or localhost:1234/ or localhost:1234/companies
            CompaniesPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignUpPage()
          },
          path("logout") {
            LogoutPage()
          },
          path("profile") {
            ProfilePage()
          },
          path("forgot") {
            ForgotPasswordPage()
          },
          path("recover") {
            RecoverPasswordPage()
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
