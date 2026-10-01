package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.domain.data.ReviewSummary
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.* // imports the type class derivation package
import sttp.tapir.json.zio.*

trait ReviewEndpoints extends BaseEndpoint {

  val createReviewEndpoint = secureBaseEndpoint
  .tag("Reviews")
  .name("Create Review")
  .description("Create a new review for a company")
  .in("reviews")
  .post
  .in(jsonBody[CreateReviewRequest])
  .out(jsonBody[Review])
  
  val getReviewByIdEndpoint = baseEndpoint
  .tag("Reviews")
  .name("Get Review by ID")
  .description("Get a review by its ID")
  .in("reviews" / path[Long]("id"))
  .get
  .out(jsonBody[Option[Review]])

  val getReviewsByCompanyIdEndpoint = baseEndpoint
  .tag("Reviews")
  .name("Get Reviews by Company ID")
  .description("Get all reviews for a specific company by its ID")
  .in("reviews" / "company" / path[Long]("companyId"))
  .get
  .out(jsonBody[List[Review]])

  val getReviewsByUserIdEndpoint = baseEndpoint
  .tag("Reviews")
  .name("Get Reviews by User ID")
  .description("Get all reviews written by a specific user by their ID")
  .in("reviews" / "user" / path[Long]("userId"))
  .get
  .out(jsonBody[List[Review]])

  val getSummaryEndpoint = baseEndpoint
  .tag("Reviews")
  .name("get summary by company id")
  .description("Get current review summary for a company by its ID")
  .in("reviews" / "company" / path[Long]("id") / "summary")
  .get
  .out(jsonBody[Option[ReviewSummary]])

  val makeSummaryEndpoint = baseEndpoint
  .tag("Reviews")
  .name("generate summary be company id")
  .description("Trigger review summary creation for a company by its ID")
  .in("reviews" / "company" / path[Long]("id") / "summary")
  .post
  .out(jsonBody[Option[ReviewSummary]])
}
