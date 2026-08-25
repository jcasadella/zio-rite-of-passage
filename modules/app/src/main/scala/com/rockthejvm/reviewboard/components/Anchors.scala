package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}

object Anchors {
  def renderNavLink(text: String, location: String, cssClass: String = "") =
    a(
      href := location,
      cls  := cssClass,
      text
    )
}
