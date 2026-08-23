package com.rockthejvm.reviewboard.config

final case class JwtConfig(issuer: String, secret: String, ttl: Long)
