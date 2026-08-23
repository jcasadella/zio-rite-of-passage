package com.rockthejvm.reviewboard.http.requests

import com.rockthejvm.reviewboard.domain.data.Company

final case class CreateCompanyRequest(
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None, // base64 encoded image, optional
    tags: List[String] = List()
) {
  def toCompany(id: Long): Company = {
    val slug = Company.makeSlug(name)
    Company(id, slug, name, url, location, country, industry, image, tags)
  }
}


object CreateCompanyRequest {
  import zio.json.JsonCodec
  import zio.json.DeriveJsonCodec

  given codec: JsonCodec[CreateCompanyRequest] = DeriveJsonCodec.gen[CreateCompanyRequest]
}
