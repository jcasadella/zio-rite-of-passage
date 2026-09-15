package com.rockthejvm.reviewboard.domain.data

import java.time.Instant

import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

final case class Review(
    id: Long,
    companyId: Long,
    userId: Long, // foreing key
    // scores
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant,
    updated: Instant
)

object Review {
  given codec: JsonCodec[Review] = DeriveJsonCodec.gen[Review]

  def empty(companyId: Long) = {
    val now = Instant.now()
    Review(
      -1L,
      companyId,
      -1L,
      5,
      5,
      5,
      5,
      5,
      "",
      now,
      now
    )
  }
}
