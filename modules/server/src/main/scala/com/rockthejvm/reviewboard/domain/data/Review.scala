package com.rockthejvm.reviewboard.domain.data

import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

import java.time.Instant

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
}
