package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.InviteNamedRecord
import com.rockthejvm.reviewboard.http.requests.InvitePackRequest
import com.rockthejvm.reviewboard.http.requests.InviteRequest
import com.rockthejvm.reviewboard.http.responses.InviteResponse
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait InviteEndpoints extends BaseEndpoint {
  val addPackEndpoint =
    secureBaseEndpoint
    .tag("Invites")
    .name("add invites")
    .description("Get pack of invites")
    .in("invites" / "packs")
    .post
    .in(jsonBody[InvitePackRequest])
    .out(stringBody)
  
  val inviteEndpoint =
    secureBaseEndpoint
    .tag("Invites")
    .name("invite")
    .description("Send people emails inviting them to leave a review")
    .in("invites" / "campaigns")
    .post
    .in(jsonBody[InviteRequest])
    .out(jsonBody[InviteResponse])
  
  val getByUserIdEndpoint =
    secureBaseEndpoint
    .tag("Invites")
    .name("get by user id")
    .description("Get all active invite packs by user")
    .get
    .in("invites" / "packs")
    .out(jsonBody[List[InviteNamedRecord]])
  
  val addPackPromotedEndpoint =
    secureBaseEndpoint
    .tag("Invites")
    .name("add invites promoted")
    .description("Get pack of invites (paid via Stripe)")
    .in("invites" / "promoted")
    .post
    .in(jsonBody[InvitePackRequest])
    .out(stringBody) // this is the Stripe checkout URL

  // webhook - will be called automatically by Stripe
  val webhookEndpoint =
    baseEndpoint
      .tag("Invites")
      .name("invite webhook")
      .description("Confirm the purchase of an invite pack")
      .in("invites" / "webhook")
      .post
      .in(header[String]("Stripe-Signature"))
      .in(stringBody)
}
