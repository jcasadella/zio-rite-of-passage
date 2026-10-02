package com.rockthejvm.reviewboard.config

final case class EmailServiceConfig(
    from: String,
    host: String,
    port: Int,
    user: String,
    password: String,
    baseUrl: String
)
