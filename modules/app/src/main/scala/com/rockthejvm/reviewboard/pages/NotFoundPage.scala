package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object NotFoundPage {
  def apply() =
    div("404 Invalid page - Are you lost?")
}
