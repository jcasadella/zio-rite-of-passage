package com.rockthejvm.reviewboard.http.controllers

import zio.*
import zio.Task
import zio.json.*
import zio.test.*

import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.services.JwtService
import com.rockthejvm.reviewboard.syntax.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val rtjvm = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(rtjvm)

    override def getAll: Task[List[Company]] = ZIO.succeed(
      List(rtjvm)
    )

    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(
      if (id == 1L) Some(rtjvm) else None
    )

    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(
      if (slug == "rock-the-jvm") Some(rtjvm) else None
    )

    override def allFilters: Task[CompanyFilter] = ZIO.succeed(
      CompanyFilter(rtjvm.location.toList, rtjvm.country.toList, rtjvm.industry.toList, rtjvm.tags)
    )

    override def search(filter: CompanyFilter): Task[List[Company]] =
      ZIO.succeed(List(rtjvm))

  }

  private val jwtServiceStub = new JwtService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.email, "AL_IS_GOOD", 999999L))
    override def verifyToken(token: String): Task[UserId] =
      ZIO.succeed(UserId(1, "me@rockthejvm.com"))
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) =
    for {
      controller <- CompanyController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post a company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
        } yield response.body

        // execute and inspect http response
        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
        }
      },
      test("get all companies") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        // execute and inspect http response
        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(rtjvm))
        }
      },
      test("get a company by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        // execute and inspect http response
        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(rtjvm)
        }
      }
    ).provide(
      ZLayer.succeed(serviceStub),
      ZLayer.succeed(jwtServiceStub)
    )
}
