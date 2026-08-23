package com.rockthejvm.reviewboard.http.controllers

import sttp.tapir.server.ServerEndpoint
import zio.Task

trait Controller {
  val routes: List[ServerEndpoint[Any, Task]]
}
