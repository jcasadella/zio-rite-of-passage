package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.*

object HttpApi {

  def gatherRoutes(controllers: List[Controller]) =
    controllers.flatMap(_.routes)

  def makeControllers = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews   <- ReviewController.makeZIO
    users     <- UserController.makeZIO
  } yield List(health, companies, reviews, users)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
