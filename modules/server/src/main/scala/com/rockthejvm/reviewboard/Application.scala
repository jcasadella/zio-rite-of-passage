package com.rockthejvm.reviewboard

import zio.*
import zio.http.Server

import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions

import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.services.CompanyService

object Application extends ZIOAppDefault {

  val serverProgram = for {
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

  override def run: ZIO[Any, Throwable, Unit] =
    serverProgram.provide(
      Server.defaultWithPort(8080),
      // services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JwtServiceLive.configuredLayer,
      HashServiceLiveArgon2.layer,
      EmailServiceLive.configuredLayer,
      OtpGeneratorLive.layer,
      // repositories
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.configuredLayer,
      // other dependencies
      Repository.dataLayer
    )
}
