package com.rockthejvm.reviewboard.core

import com.rockthejvm.reviewboard.config.BackendClientConfig
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

case class RestrictedEndpointException(msg: String) extends RuntimeException(msg)

trait BackendClient {
  val companies: CompanyEndpoints
  val user: UserEndpoints
  val reviews: ReviewEndpoints

  def endpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O]
  def secureEndpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
  ) extends BackendClient {
      override val companies: CompanyEndpoints = new CompanyEndpoints {}
      override val user: UserEndpoints = new UserEndpoints {}
      override val reviews: ReviewEndpoints = new ReviewEndpoints {}
      
      private def endpointRequest[I, E, O](
        endpoint: Endpoint[Unit, I, E, O, Any]
      ): I => Request[Either[E, O], Any] =
        interpreter
          .toRequestThrowDecodeFailures(endpoint, config.uri)
      private def secureEndpointRequest[S, I, E, O](
        endpoint: Endpoint[S, I, E, O, Any]
      ): S => I => Request[Either[E, O], Any] =
        interpreter
          .toSecureRequestThrowDecodeFailures(endpoint, config.uri)

      private def tokenOrFail =
        ZIO
        .fromOption(Session.getUserState)
        .orElseFail(RestrictedEndpointException("You need to log in"))
        .map(_.token)
      
      override def endpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O] =
        backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve

      override def secureEndpointRequestZio[I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])(payload: I): Task[O] =
        for {
          token <- tokenOrFail
          response <- backend.send(secureEndpointRequest(endpoint)(token)(payload)).map(_.body).absolve
        } yield response
    
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
