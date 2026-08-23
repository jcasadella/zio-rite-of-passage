package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

object HashServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("HashServiceSpec")(
      test("hash and verify plain text") {
        for {
          service     <- ZIO.service[HashService]
          hash        <- service.hash("plain text to hash")
          verified    <- service.verify("plain text to hash", hash)
          notVerified <- service.verify("another plain text", hash)
        } yield assertTrue(
          verified && !notVerified
        )
      }
    ).provide(
      HashServiceLiveArgon2.layer
    )
}
