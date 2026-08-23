package com.rockthejvm.reviewboard.domain.errors

abstract class ApplicationException(message: String) extends RuntimeException(message)

object UnauthorizedException extends ApplicationException("Unauthorized")
