package com.rockthejvm.reviewboard.http.controllers

import zio.*

import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.services.JwtService

import collection.mutable

class CompanyController private (service: CompanyService, jwtService: JwtService)
    extends Controller
    with CompanyEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        service.create(req).either
      }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic { _ =>
      service.getAll.either
    }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome { case _: NumberFormatException =>
          service.getBySlug(id)
        }
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = for {
    companyService <- ZIO.service[CompanyService]
    jwtService     <- ZIO.service[JwtService]
  } yield new CompanyController(companyService, jwtService)
}
