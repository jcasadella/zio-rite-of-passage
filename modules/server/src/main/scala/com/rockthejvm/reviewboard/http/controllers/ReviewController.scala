package com.rockthejvm.reviewboard.http.controllers

import zio.*

import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.JwtService
import com.rockthejvm.reviewboard.services.ReviewService

class ReviewController private (reviewService: ReviewService, jwtService: JwtService)
    extends Controller
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createReviewEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        reviewService.create(req, userId.id).either
      }

  val getById: ServerEndpoint[Any, Task] =
    getReviewByIdEndpoint.serverLogic(id => reviewService.getById(id).either)

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getReviewsByCompanyIdEndpoint.serverLogic(companyId =>
      reviewService.getByCompanyId(companyId).either
    )

  val getByUserId: ServerEndpoint[Any, Task] =
    getReviewsByUserIdEndpoint.serverLogic(userId => reviewService.getByUserId(userId).either)

  val getSummary: ServerEndpoint[Any, Task] =
    getSummaryEndpoint.serverLogic(companyId => reviewService.getSummary(companyId).either)

  val makeSummary: ServerEndpoint[Any, Task] =
    makeSummaryEndpoint.serverLogic(companyId => reviewService.makeSummary(companyId).either)

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getById, getSummary, makeSummary, getByCompanyId, getByUserId)
}

object ReviewController {
  val makeZIO = for {
    service    <- ZIO.service[ReviewService]
    jwtService <- ZIO.service[JwtService]
  } yield new ReviewController(service, jwtService)
}
