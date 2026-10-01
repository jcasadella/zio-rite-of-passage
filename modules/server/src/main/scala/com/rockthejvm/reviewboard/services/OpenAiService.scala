package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.OpenAiConfig
import com.rockthejvm.reviewboard.http.endpoints.OpenAiEndpoints
import com.rockthejvm.reviewboard.http.requests.CompletionRequest
import sttp.capabilities.zio.ZioStreams
import sttp.client3.Request
import sttp.client3.SttpBackend
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

trait OpenAiService {
  def getCompletion(prompt: String): Task[Option[String]]
}

class OpenAiServiceLive private (
  backend: SttpBackend[Task, ZioStreams],
  interpreter: SttpClientInterpreter,
  config: OpenAiConfig
) extends OpenAiService with OpenAiEndpoints {

  private def secureEndpointRequest[S, I, E, O](
        endpoint: Endpoint[S, I, E, O, Any]
      ): S => I => Request[Either[E, O], Any] =
        interpreter
          .toSecureRequestThrowDecodeFailures(endpoint, Uri.parse(config.baseUrl).toOption)

  def secureEndpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])(payload: I): Task[O] =
    backend.send(secureEndpointRequest(endpoint)(config.secretKey)(payload)).map(_.body).absolve

  override def getCompletion(prompt: String): Task[Option[String]] =
    secureEndpointRequestZio(completionsEndpoint)(CompletionRequest.singleRequest(prompt))
      .map(response => response.choices.headOption.map(_.message.content))
}

object OpenAiServiceLive {
  val layer = ZLayer(
    for {
      backend <- ZIO.service[SttpBackend[Task, ZioStreams]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config <- ZIO.service[OpenAiConfig]
    } yield new OpenAiServiceLive(backend, interpreter, config)
  )

  val configuredLayer =
    HttpClientZioBackend.layer() >+>
      ZLayer.succeed(SttpClientInterpreter()) >+>
      Configs.makeLayer[OpenAiConfig]("rockthejvm.openAi") >>> layer
}

object OpenAiServiceDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    ZIO.service[OpenAiService]
      .flatMap(_.getCompletion("Please write a potential expansion of the acronym RTJVM, in one sentence."))
      .flatMap(resp => Console.printLine(resp.toString))
      .provide(OpenAiServiceLive.configuredLayer)
}
