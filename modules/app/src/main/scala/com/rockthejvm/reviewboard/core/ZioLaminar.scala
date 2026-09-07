package com.rockthejvm.reviewboard.core

import zio.*

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import com.rockthejvm.reviewboard.config.BackendClientConfig

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object ZioLaminar {

  def useBackend = ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A])
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .service[BackendClient]
        .flatMap(_.endpointRequestZio(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)
}
