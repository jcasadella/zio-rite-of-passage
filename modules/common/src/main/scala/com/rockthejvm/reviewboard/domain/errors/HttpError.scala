package com.rockthejvm.reviewboard.domain.errors

import sttp.model.StatusCode

final case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable
) extends RuntimeException(message, cause)

object HttpError {
  def decode(tuple: (StatusCode, String)): HttpError = {
    val (statusCode, message) = tuple
    HttpError(statusCode, message, new RuntimeException(message))
  }

  def encode(error: Throwable): (StatusCode, String) = {
    error match {
      case UnauthorizedException(msg) => (StatusCode.Unauthorized, msg)
      case httpError: HttpError       => (httpError.statusCode, httpError.message)
      case _                          => (StatusCode.InternalServerError, error.getMessage())
    }
  }
}
