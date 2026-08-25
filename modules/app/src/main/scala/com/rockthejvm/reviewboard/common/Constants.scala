package com.rockthejvm.reviewboard.common

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {

  @js.native
  @JSImport("url:../../static/img/fiery-lava 128x128.png", JSImport.Default)
  private object LogoImage extends js.Object

  val logoImage: String = LogoImage.asInstanceOf[String]

  @js.native
  @JSImport("url:../../static/img/generic_company.png", JSImport.Default)
  private object GenericCompanyImage extends js.Object

  val genericCompanyImage: String = GenericCompanyImage.asInstanceOf[String]

}
