package com.rockthejvm.reviewboard.core

import com.rockthejvm.reviewboard.config.BackendClientConfig
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

trait BackendClient {
  val companies: CompanyEndpoints
  val user: UserEndpoints

  def endpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
  ) extends BackendClient {
      override val companies: CompanyEndpoints = new CompanyEndpoints {}
      override val user: UserEndpoints = new UserEndpoints {}
      private def endpointRequest[I, E, O](
        endpoint: Endpoint[Unit, I, E, O, Any]
      ): I => Request[Either[E, O], Any] =
        interpreter
          .toRequestThrowDecodeFailures(endpoint, config.uri)
      
      override def endpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O] =
        backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve
    
}

case class AppBackend(underlying: SttpBackend[Task, ZioStreams & WebSockets])

object BackendClientLive {

  val layer = ZLayer {
    for {
      backend     <- ZIO.service[AppBackend]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(backend.underlying, interpreter, config)
  }

  val configuredLayer = {
    val backend = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val config = BackendClientConfig(Some(uri"http://localhost:8080"))

    ZLayer.succeed(AppBackend(backend)) ++
      ZLayer.succeed(interpreter) ++
      ZLayer.succeed(config) >>> layer
  }
}
