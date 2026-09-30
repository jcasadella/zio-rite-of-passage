package com.rockthejvm.reviewboard.components

import com.rockthejvm.reviewboard.pages.*

import com.raquo.laminar.api.L.{*, given}
import frontroute.*
import org.scalajs.dom

object Router {
  val externalUrlBus = EventBus[String]()

  def apply() =
    mainTag( // <main>
      onMountCallback(ctx =>
        externalUrlBus.events.foreach(url => dom.window.location.href = url)(ctx.owner)
      ),
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
          path("changepassword") {
            ChangePasswordPage()
          },
          path("forgot") {
            ForgotPasswordPage()
          },
          path("recover") {
            RecoverPasswordPage()
          },
          path("post") {
            CreateCompanyPage()
          },
          path("company" / segment) { companySlug =>
            CompanyPage(companySlug)
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
