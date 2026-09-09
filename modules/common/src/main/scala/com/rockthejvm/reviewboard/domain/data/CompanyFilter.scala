package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class CompanyFilter(
    locations: List[String] = List(),
    countries: List[String] = List(),
    industries: List[String] = List(),
    tags: List[String] = List()
) derives JsonCodec {
  val isEmpty: Boolean =
    locations.isEmpty && countries.isEmpty && industries.isEmpty && tags.isEmpty
}

object CompanyFilter {
  val empty = CompanyFilter()
}

case class CompanyFilter2(checks: Map[String, Map[String, Boolean]] = Map())

object Foo {
  val a = CompanyFilter2()
  val b = a.checks
}
