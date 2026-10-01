package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.errors.HttpError
import com.rockthejvm.reviewboard.http.requests.CompletionRequest
import com.rockthejvm.reviewboard.http.responses.CompletionResponse
import sttp.client3.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait OpenAiEndpoints extends BaseEndpoint {

  val completionsEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)
    .securityIn(auth.bearer[String]())
    .in("v1" / "chat" / "completions")
    .post
    .in(jsonBody[CompletionRequest])
    .out(jsonBody[CompletionResponse])

}
