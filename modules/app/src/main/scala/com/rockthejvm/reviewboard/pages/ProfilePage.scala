package com.rockthejvm.reviewboard.pages

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.components.InviteActions
import com.rockthejvm.reviewboard.core.Session

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object ProfilePage {

  def apply() =
    div(
      cls := "row",
      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            src := Constants.logoImage,
            alt := "Rock the JVM"
          )
        )
      ),
      div(
        cls := "col-md-7",
        // right
        div(
          cls := "form-section",
          div(
            child <-- Session.userState.signal.map {
              case None    => renderInvalid()
              case Some(_) => renderContent()
            }
          )
        )
      )
    )

  private def renderInvalid() =
    div(
      cls := "top-section",
      h1(span("Oops!")),
      div("It seems you are not logged in.")
    )

  private def renderContent() =
    div(
      cls := "top-section",
      h1(span("Profile")),
      div(
        cls := "profile-section",
        h3(span("Account Settings")),
        Anchors.renderNavLink("Change Password", "/changepassword")
      ),
      InviteActions()
    )
}
