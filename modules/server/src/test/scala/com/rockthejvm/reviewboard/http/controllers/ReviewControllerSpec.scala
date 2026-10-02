package com.rockthejvm.reviewboard.http.controllers

import java.time.Instant

import zio.*
import zio.Task
import zio.json.*
import zio.test.*

import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.domain.data.ReviewSummary
import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.controllers.ReviewController
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.services.JwtService
import com.rockthejvm.reviewboard.services.ReviewService
import com.rockthejvm.reviewboard.syntax.*

object ReviewControllerSpec extends ZIOSpecDefault {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 5,
    review = "Great company!",
    created = Instant.now(),
    updated = Instant.now()
  )
  private val serviceStub = new ReviewService {
    override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)
    override def getById(id: Long): Task[Option[Review]] =
      ZIO.succeed(if (id == 1L) Some(goodReview) else None)
    override def getByCompanyId(companyId: Long): Task[List[Review]] =
      ZIO.succeed(if (companyId == 1L) List(goodReview) else List.empty)
    override def getByUserId(userId: Long): Task[List[Review]] =
      ZIO.succeed(if (userId == 1L) List(goodReview) else List.empty)
    override def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
      ZIO.none
    override def makeSummary(companyId: Long): Task[Option[ReviewSummary]] =
      ZIO.none
  }

  private val jwtServiceStub = new JwtService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.id, user.email, "AL_IS_GOOD", 999999L))
    override def verifyToken(token: String): Task[UserId] =
      ZIO.succeed(UserId(1, "me@rockthejvm.com"))
  }

  private def backendStubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) =
    for {
      controller <- ReviewController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewControllerSpec")(
      test("post a review") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/reviews")
            .body(
              CreateReviewRequest(
                companyId = 1L,
                management = 5,
                culture = 5,
                salary = 5,
                benefits = 5,
                wouldRecommend = 5,
                review = "Great company!"
              ).toJson
            )
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
        } yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        )
      },
      test("get a review by id") {
        for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/reviews/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/2")
            .send(backendStub)
        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .isEmpty
        }
      },
      test("get reviews by company id") {
        for {
          backendStub <- backendStubZIO(_.getByCompanyId)
          response <- basicRequest
            .get(uri"/reviews/company/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/company/2")
            .send(backendStub)
        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(goodReview)) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List.empty)
        }
      },
      test("get reviews by user id") {
        for {
          backendStub <- backendStubZIO(_.getByUserId)
          response <- basicRequest
            .get(uri"/reviews/user/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/user/2")
            .send(backendStub)
        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(goodReview)) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List.empty)
        }
      }
    ).provide(
      ZLayer.succeed(serviceStub),
      ZLayer.succeed(jwtServiceStub)
    )
}
