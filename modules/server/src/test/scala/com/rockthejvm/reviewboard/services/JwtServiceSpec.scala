package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.config.JwtConfig
import com.rockthejvm.reviewboard.domain.data.User

object JwtServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JwtServiceSpec")(
      test("create and validate token") {
        for {
          service   <- ZIO.service[JwtService]
          userToken <- service.createToken(User(1L, "me@rockthejvm.com", "unimportant"))
          userId    <- service.verifyToken(userToken.token)
        } yield assertTrue(
          userId.id == 1L &&
            userId.email == "me@rockthejvm.com"
        )
      }
    ).provide(
      JwtServiceLive.layer,
      ZLayer.succeed(JwtConfig("testapp", "secret", 60))
    )
}
