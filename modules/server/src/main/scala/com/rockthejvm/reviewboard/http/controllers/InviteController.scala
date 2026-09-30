package com.rockthejvm.reviewboard.http.controllers

import zio.*

import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.http.endpoints.InviteEndpoints
import com.rockthejvm.reviewboard.http.responses.InviteResponse
import com.rockthejvm.reviewboard.services.InviteService
import com.rockthejvm.reviewboard.services.JwtService
import com.rockthejvm.reviewboard.services.PaymentService

class InviteController private (
    inviteService: InviteService,
    jwtService: JwtService,
    paymentService: PaymentService
) extends Controller
    with InviteEndpoints {

  val addPack: ServerEndpoint[Any, Task] =
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

  val invite: ServerEndpoint[Any, Task] =
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

  val getByUserId: ServerEndpoint[Any, Task] =
    getByUserIdEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => _ =>
        inviteService.getByUserId(userId.email).either
      }

  val addPackPromoted: ServerEndpoint[Any, Task] =
    addPackPromotedEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        inviteService
          .addInvitePack(userId.email, req.companyId)
          .flatMap { packId =>
            paymentService.createCheckoutSession(packId, userId.email)
          }
          .someOrFail(new RuntimeException("Cannot create payment checkout session"))
          .map(_.getUrl()) // the checkout session URL
          .either
      }

  val webhook: ServerEndpoint[Any, Task] =
    webhookEndpoint
      .serverLogic { (signature, payload) =>
        paymentService
          .handleWebhookEvent(
            signature,
            payload,
            packId => inviteService.activatePack(packId.toLong)
          )
          .unit
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(addPack, addPackPromoted, webhook, invite, getByUserId)

}

object InviteController {
  val makeZIO = for {
    inviteService  <- ZIO.service[InviteService]
    jwtService     <- ZIO.service[JwtService]
    paymentService <- ZIO.service[PaymentService]
  } yield new InviteController(inviteService, jwtService, paymentService)
}
