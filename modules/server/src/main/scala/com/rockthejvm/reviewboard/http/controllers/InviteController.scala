package com.rockthejvm.reviewboard.http.controllers

import zio.*

import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.http.endpoints.InviteEndpoints
import com.rockthejvm.reviewboard.http.responses.InviteResponse
import com.rockthejvm.reviewboard.services.InviteService
import com.rockthejvm.reviewboard.services.JwtService

class InviteController private (inviteService: InviteService, jwtService: JwtService)
    extends Controller
    with InviteEndpoints {

  val addPack =
    addPackEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        {
          Console.printLine(s"READING REQUEST= ${req.toString}") *>
            inviteService
              .addInvitePack(userId.email, req.companyId)
              .map(_.toString)
              .either
        }
      }

  val invite =
    inviteEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        inviteService
          .sendInvites(userId.email, req.companyId, req.emails)
          .map { nInvitesSent =>
            if (nInvitesSent == req.emails.size) InviteResponse("ok", nInvitesSent)
            else InviteResponse("partial success", nInvites = nInvitesSent)
          }
          .either
      }

  val getByUserId =
    getByUserIdEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => _ =>
        inviteService.getByUserId(userId.email).either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(addPack, invite, getByUserId)

}

object InviteController {
  val makeZIO = for {
    inviteService <- ZIO.service[InviteService]
    jwtService    <- ZIO.service[JwtService]
  } yield new InviteController(inviteService, jwtService)
}
