package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.RecoveryTokensConfig
import com.rockthejvm.reviewboard.domain.data.PasswordRecoveryToken
import com.rockthejvm.reviewboard.services.OtpGenerator
import io.getquill.*
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

trait RecoveryTokensRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokensRepositoryLive private (tokenConfig: RecoveryTokensConfig, quill: Quill.Postgres[SnakeCase], userRepository: UserRepository, otpGenerator: OtpGenerator, clock: java.time.Clock) extends RecoveryTokensRepository {
  import quill.* 

  inline given schema: SchemaMeta[PasswordRecoveryToken] = schemaMeta[PasswordRecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given upMeta: UpdateMeta[PasswordRecoveryToken] = updateMeta[PasswordRecoveryToken](_.email)

  private def findToken(email: String): Task[Option[String]] =
    run(query[PasswordRecoveryToken].filter(_.email == lift(email)).map(_.token)).map(_.headOption)

  private def replaceToken(email: String): Task[String] =
    for {
      token <- otpGenerator.generateToken
      _ <- run(
        query[PasswordRecoveryToken].updateValue(lift(PasswordRecoveryToken(email, token, clock.millis() + tokenConfig.duration)))
        .returning(r => r)
        )
    } yield token
  private def generateToken(email: String): Task[String] =
    for {
      token <- otpGenerator.generateToken
      _ <- run(
        query[PasswordRecoveryToken].insertValue(lift(PasswordRecoveryToken(email, token, clock.millis() + tokenConfig.duration)))
        .returning(r => r)
        )
    } yield token

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None => generateToken(email)
    }

  override def getToken(email: String): Task[Option[String]] =
    for {
      existingUser <- userRepository.getByEmail(email)
      token <- makeFreshToken(email).when(existingUser.nonEmpty)
    } yield token

  override def checkToken(email: String, token: String): Task[Boolean] = for {
    now <- Clock.instant
    checkValid <- run(
      query[PasswordRecoveryToken]
      .filter(r => r.email == lift(email) && r.token == lift(token) && r.expiration > lift(now.toEpochMilli()))
      ).map(_.nonEmpty)
  } yield checkValid
    

}

object RecoveryTokensRepositoryLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[RecoveryTokensConfig]
      quill <- ZIO.service[Quill.Postgres[SnakeCase]]
      userRepository <- ZIO.service[UserRepository]
      otpGenerator <- ZIO.service[OtpGenerator]
      clock <- Clock.javaClock
    } yield new RecoveryTokensRepositoryLive(config, quill, userRepository, otpGenerator, clock)
  }

  val configuredLayer = Configs.makeLayer[RecoveryTokensConfig]("rockthejvm.recoveryTokens") >>> layer
}
