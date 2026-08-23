package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Company(
    id: Long,
    slug: String,
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None, // base64 encoded image, optional
    tags: List[String] = List()
)

object Company {
  given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

  def makeSlug(name: String): String = {
    name
    .replaceAll(" +", " ") // replace multiple spaces with a single space
    .split(" ")
    .map(_.toLowerCase())
    .mkString("-")
  }
}
