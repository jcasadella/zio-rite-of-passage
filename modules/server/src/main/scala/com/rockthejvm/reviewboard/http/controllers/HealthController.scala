package com.rockthejvm.reviewboard.http.controllers

import zio.*

import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.errors.HttpError
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint

class HealthController private extends Controller with HealthEndpoint {

  val health = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val errorHealth = errorEndpoint
    .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom!")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, errorHealth)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
