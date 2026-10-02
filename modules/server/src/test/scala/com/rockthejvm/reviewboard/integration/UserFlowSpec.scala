package com.rockthejvm.reviewboard.integration

import com.rockthejvm.reviewboard.config.RecoveryTokensConfig
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.http.requests.DeleteAccountRequest
import com.rockthejvm.reviewboard.http.requests.ForgotPasswordRequest
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import com.rockthejvm.reviewboard.http.requests.RecoverPasswordRequest
import com.rockthejvm.reviewboard.http.requests.RegisterUserAccount
import com.rockthejvm.reviewboard.http.requests.UpdatePasswordRequest
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.repositories.RecoveryTokensRepositoryLive
import com.rockthejvm.reviewboard.repositories.Repository
import com.rockthejvm.reviewboard.repositories.RepositorySpec
import com.rockthejvm.reviewboard.repositories.UserRepository
import com.rockthejvm.reviewboard.repositories.UserRepositoryLive
import com.rockthejvm.reviewboard.services.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.*
import sttp.monad.MonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/integration.sql"

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] = {
      val apiPath = s"/api/${path.stripPrefix("/")}"
      basicRequest
        .method(method, uri"$apiPath")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))
    }

    def post[B: JsonCodec](
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, maybeToken)

    def postNoResponse(path: String, payload: A): Task[Unit] = {
      val apiPath = s"/api/${path.stripPrefix("/")}"
      basicRequest
        .method(Method.POST, uri"$apiPath")
        .body(payload.toJson)
        .send(backend)
        .map(_ => ())
    }

    def put[B: JsonCodec](
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, maybeToken)

    def delete[B: JsonCodec](
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, maybeToken)

  }

  class EmailServiceProbe extends EmailService("http://localhost:8080") {
    val db = collection.mutable.Map[String, String]()
    override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit
    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed(db.addOne(to, token))

    def probeToken(email: String): Task[Option[String]] = ZIO.succeed(db.get(email))
  }
  val emailServiceLayer: ZLayer[Any, Nothing, EmailServiceProbe] = ZLayer.succeed(new EmailServiceProbe)

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("create user") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              RegisterUserAccount("me@rockthejvm.com", "rockthejvm")
            )
        } yield assertTrue(maybeResponse.contains(UserResponse("me@rockthejvm.com")))
      },
      test("create and log in") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              RegisterUserAccount("me@rockthejvm.com", "rockthejvm")
            )
          maybeToken <- basicRequest
            .post(uri"/api/users/login")
            .body(LoginRequest("me@rockthejvm.com", "rockthejvm").toJson)
            .send(backendStub)
            .map(_.body)
            .map(_.toOption.flatMap(payload => payload.fromJson[UserToken].toOption))
        } yield assertTrue(maybeToken.exists(_.email == "me@rockthejvm.com"))
      },
      test("change password") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              RegisterUserAccount("me@rockthejvm.com", "rockthejvm")
            )
          maybeToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "rockthejvm"))
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .put[UserResponse](
              "/users/password",
              UpdatePasswordRequest("me@rockthejvm.com", "rockthejvm", "scalarulez"),
              Some(maybeToken.token)
            )
          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "rockthejvm"))
          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "scalarulez"))
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.exists(_.email == "me@rockthejvm.com")
        )
      },
      test("delete user") {
        for {
          backendStub <- backendStubZIO
          userRepo    <- ZIO.service[UserRepository]
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              RegisterUserAccount("me@rockthejvm.com", "rockthejvm")
            )
          maybeOldUser <- userRepo.getByEmail("me@rockthejvm.com")
          maybeToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "rockthejvm"))
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .delete[UserResponse](
              "/users",
              DeleteAccountRequest("me@rockthejvm.com", "rockthejvm"),
              Some(maybeToken.token)
            )
          maybeToken2 <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "rockthejvm"))
          maybeUser <- userRepo.getByEmail("me@rockthejvm.com")
        } yield assertTrue(
          maybeOldUser.exists(_.email == "me@rockthejvm.com") &&
            maybeToken2.isEmpty &&
            maybeUser.isEmpty
        )
      },
      test("recover password flow")(
        for {
          backendStub <- backendStubZIO
          _ <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("me@rockthejvm.com", "rockthejvm")
          )
          _ <- backendStub.postNoResponse(
            "/users/forgot",
            ForgotPasswordRequest("me@rockthejvm.com")
          )
          // _ <- backendStub.post[Unit]("", )
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          token <- emailServiceProbe.probeToken("me@rockthejvm.com")
          .someOrFail(new RuntimeException("token was not emailed"))
          _ <- backendStub.postNoResponse("/users/recover", RecoverPasswordRequest("me@rockthejvm.com", token, "scalarulez"))
          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "rockthejvm"))
          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("me@rockthejvm.com", "scalarulez"))
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.exists(_.email == "me@rockthejvm.com")
        )
      )
    ).provide(
      UserServiceLive.layer,
      JwtServiceLive.configuredLayer,
      HashServiceLiveArgon2.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.layer,
      OtpGeneratorLive.layer,
      emailServiceLayer,
      Repository.quillLayer,
      datasourceLayer,
      ZLayer.succeed(RecoveryTokensConfig(24 * 3600))
      // Scope.default
    )
}
