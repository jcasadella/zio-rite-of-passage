package com.rockthejvm.reviewboard.services

import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.JwtConfig
import com.rockthejvm.reviewboard.domain.data.*
import zio.*
import zio.config.typesafe.TypesafeConfigProvider

import java.time.Instant

trait JwtService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JwtServiceLive (jwtConfig: JwtConfig, clock: java.time.Clock) extends JwtService {
  private val Claim_Username = "username"

  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)
  private val verifier: JWTVerifier = JWT.require(algorithm)
      .withIssuer(jwtConfig.issuer)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] = for {
    now <- ZIO.attempt(clock.instant())
    expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
    token <- ZIO.attemptBlocking(
      JWT
        .create()
        .withIssuer(jwtConfig.issuer)
        .withIssuedAt(now)
        .withExpiresAt(expiration)
        .withSubject(user.id.toString()) // user identifier
        .withClaim(Claim_Username, user.email)
        .sign(algorithm)
      )
    } yield UserToken(user.email, token, expiration.getEpochSecond())


  override def verifyToken(token: String): Task[UserId] = for {
    decoded <- ZIO.attempt(verifier.verify(token))
    userId <- ZIO.attempt(
      UserId(decoded.getSubject().toLong, decoded.getClaim(Claim_Username).asString())
    )
  } yield userId
}

object JwtServiceLive {
  val layer = ZLayer {
    for {
      jwtConfig <- ZIO.service[JwtConfig]
      clock <- Clock.javaClock
    } yield new JwtServiceLive(jwtConfig, clock)
  }

  val configuredLayer =
    Configs.makeLayer[JwtConfig]("rockthejvm.jwt") >>> layer
}

object JwtServiceDemo extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] = {

    val program = for {
      service <- ZIO.service[JwtService]
      userToken <- service.createToken(User(1L, "me@rockthejvm.com", "unimportant"))
      _ <- Console.printLine(userToken)
      userId <- service.verifyToken(userToken.token)
      _ <- Console.printLine(userId.toString())
    } yield ()
    program.provide(
      JwtServiceLive.layer,
      Configs.makeLayer[JwtConfig]("rockthejvm.jwt")
    )
  }
}
