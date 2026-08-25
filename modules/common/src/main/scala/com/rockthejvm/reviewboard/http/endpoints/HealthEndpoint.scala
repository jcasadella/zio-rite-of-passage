package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint extends BaseEndpoint {
  val healthEndpoint = baseEndpoint
    .tag("health")
    .name("health")
    .description("health check")
    .get          // http method
    .in("health") // path
    .out(plainBody[String])
  
  val errorEndpoint = baseEndpoint
    .tag("health")
    .name("error health")
    .description("health check - should fail")
    .get          // http method
    .in("health" / "error") // path
    .out(plainBody[String])
}
