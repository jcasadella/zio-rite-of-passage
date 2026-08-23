package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.* // imports the type class derivation package
import sttp.tapir.json.zio.*

trait CompanyEndpoints extends BaseEndpoint {
    val createEndpoint =
        secureBaseEndpoint
        .tag("companies")
        .name("create")
        .description("Create a listing for a company")
        .in("companies")
        .post
        .in(jsonBody[CreateCompanyRequest])
        .out(jsonBody[Company])

    val getAllEndpoint =
        baseEndpoint
        .tag("companies")
        .name("getAll")
        .description("Get all companies")
        .in("companies")
        .get
        .out(jsonBody[List[Company]])

    val getByIdEndpoint =
        baseEndpoint
        .tag("companies")
        .name("getById")
        .description("Get a company by its id (or maybe by slug in the future)") // TODO
        .in("companies" / path[String]("id"))
        .get
        .out(jsonBody[Option[Company]])
}
