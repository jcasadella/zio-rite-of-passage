package com.rockthejvm.reviewboard.http.responses

import zio.json.JsonCodec

case class CompletionMessage(
    role: String,
    content: String
) derives JsonCodec

case class Completion(
    index: Int,
    message: CompletionMessage
) derives JsonCodec

final case class CompletionResponse(
    id: String,
    created: Long,
    choices: List[Completion]
) derives JsonCodec
