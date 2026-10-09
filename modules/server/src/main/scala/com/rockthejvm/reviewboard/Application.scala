package com.rockthejvm.reviewboard

import java.net.InetSocketAddress

import zio.*
import zio.http.Server

import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions

import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.HttpConfig
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.services.CompanyService

object Application extends ZIOAppDefault {

  val configuredServer =
    Configs.makeLayer[HttpConfig]("rockthejvm.http") >>>
      ZLayer(
        ZIO
          .service[HttpConfig]
          .map(config => Server.Config.default.copy(address = InetSocketAddress(config.port)))
      ) >>> Server.live

  def runMigrations = for {
    flyway <- ZIO.service[FlywayService]
    _ <- flyway.runMigrations.catchSome { case e =>
      ZIO.logError("MIGRATIONS FAILED: " + e) *>
        flyway.runRepairs *> flyway.runMigrations
    }
  } yield ()

  def startServer = for {
    endpoints <- HttpApi.endpointsZIO
    _         <- Console.printLine(endpoints.map(_.show).mkString("\n"))
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default.appendInterceptor(
          CORSInterceptor.default
        )
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  def program = for {
    _ <- ZIO.log("Rock the JVM! Bootstrapping...")
    _ <- runMigrations
    _ <- startServer
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    program.provide(
      configuredServer,
      // services
      CompanyServiceLive.layer,
      ReviewServiceLive.configuredLayer,
      UserServiceLive.layer,
      InviteServiceLive.configuredLayer,
      JwtServiceLive.configuredLayer,
      HashServiceLiveArgon2.layer,
      EmailServiceLive.configuredLayer,
      OtpGeneratorLive.layer,
      PaymentServiceLive.configuredLayer,
      OpenAiServiceLive.configuredLayer,
      FlywayServiceLive.configuredLayer,
      // repositories
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      InviteRepositoryLive.layer,
      RecoveryTokensRepositoryLive.configuredLayer,
      // other dependencies
      Repository.dataLayer
    )
}
