package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

case class CompletionMessage(
    content: String,
    role: String = "user"
) derives JsonCodec

final case class CompletionRequest(
    messages: List[CompletionMessage],
    model: String = "gpt-6-luna"
) derives JsonCodec

object CompletionRequest {
  def singleRequest(prompt: String) = CompletionRequest(
    messages = List(
      CompletionMessage(prompt)
    )
  )
}
