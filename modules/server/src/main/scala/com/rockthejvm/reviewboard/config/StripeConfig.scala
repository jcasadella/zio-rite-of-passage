package com.rockthejvm.reviewboard.config

final case class StripeConfig(
    secretKey: String,
    invitesWebhookSecret: String,
    invitePackPriceId: String,
    successUrl: String,
    cancelUrl: String
)
