package com.rockthejvm.reviewboard.pages

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.*
import org.scalajs.dom.html.Element

case class CreateCompanyState(
    name: String = "",
    url: String = "",
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None, // base64 encoded image, optional
    tags: List[String] = List(),
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  override def errorList: List[Option[String]] = List(
    Option.when(name.isEmpty())("The name cannot be empty"),
    Option.when(!url.matches(Constants.urlRegex))("The URL is invalid")
  ) ++ upstreamStatus.map(_.left.toOption).toList

  override def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)

  def toRequest: CreateCompanyRequest = CreateCompanyRequest(
    name,
    url,
    location,
    country,
    industry,
    image,
    tags
  )
}

object CreateCompanyPage extends FormPage[CreateCompanyState]("Post a Company") {

  override def basicState: CreateCompanyState = CreateCompanyState()

  val submitter = Observer[CreateCompanyState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.companies.createEndpoint(state.toRequest))
        .map { company =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus =
                Some(Right(s"Company ${company.name} posted! Check it in the companies list."))
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
      "Company Name",
      "name-input",
      "text",
      true,
      "Company Name",
      (s, v) => s.copy(name = v, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Company URL",
      "url-input",
      "text",
      true,
      "Company URL",
      (s, v) => s.copy(url = v, showStatus = false, upstreamStatus = None)
    ),
    renderLogoUpload("Company Logo", "logo-upload"),
    renderInput(
      "Location",
      "location-input",
      "text",
      false,
      "Location",
      (s, v) => s.copy(location = Some(v), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Country",
      "country-input",
      "text",
      false,
      "Country",
      (s, v) => s.copy(country = Some(v), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Industry",
      "industry-input",
      "text",
      false,
      "Industry",
      (s, v) => s.copy(industry = Some(v), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Tags - separate by ','",
      "tags-input",
      "text",
      false,
      "Scala, zio",
      (s, v) =>
        s.copy(tags = v.split(",").map(_.trim).toList, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Post Company",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  val fileUploader = (files: List[File]) => {
    val maybeFile = files.headOption.filter(_.size > 0)
    maybeFile.foreach { file =>
      val reader = new FileReader
      reader.onload = _ => {
        val fakeImage = document.createElement("img").asInstanceOf[HTMLImageElement]
        fakeImage.addEventListener(
          "load",
          _ => {
            val canvas          = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
            val context         = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
            val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
            canvas.width = width
            canvas.height = height
            context.drawImage(fakeImage, 0, 0, width, height)
            stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
          }
        )
        fakeImage.src = reader.result.toString
      }
      reader.readAsDataURL(file)
    }
  }

  private val MaxImageDimension = 256
  private def computeDimensions(width: Int, height: Int): (Int, Int) =
    if (width >= height) {
      val ratio = width * 1.0 / MaxImageDimension
      val newW  = width / ratio
      val newH  = height / ratio
      (newW.toInt, newH.toInt)
    } else {
      val (newH, newW) = computeDimensions(height, width)
      (newW, newH)
    }

  private def renderLogoUpload(name: String, uid: String, isRequired: Boolean = false) =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          div(
            cls := "image-upload",
            input(
              `type` := "file",
              cls    := "form-control",
              idAttr := uid,
              accept := "image/*",
              onChange.mapToFiles --> (files => (fileUploader(files)))
            ),
            img(
              cls := "image-upload-thumbnail",
              src <-- stateVar.signal.map(_.image.getOrElse(Constants.genericCompanyImage))
            )
          )
        )
      )
    )
}
