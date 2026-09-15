package com.rockthejvm.reviewboard.components

import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*


@js.native
@JSImport("showdown", JSImport.Default)
object MarkdownLib extends js.Object {

  @js.native
  class Converter extends js.Object { // class name DOES matter here
    def makeHtml(text: String): String = js.native
  }
}
object Markdown {
  def toHtml(text: String) =
    new MarkdownLib.Converter().makeHtml(text)
}
