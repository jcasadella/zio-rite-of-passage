package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.domain.data.ReviewSummary
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.syntax.*

object ReviewServiceSpec extends ZIOSpecDefault {

  val stubRepoLayer = ZLayer.succeed(
    new ReviewRepository {
      override def create(
          review: Review
      ): Task[Review] = ZIO.succeed(goodReview)
      override def getById(id: Long): Task[Option[Review]] = ZIO.succeed {
        id match {
          case 1L => Some(goodReview)
          case 2L => Some(badReview)
          case _  => None
        }
      }
      override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
        if (companyId == 1L) List(goodReview, badReview)
        else List.empty
      }

      override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
        if (userId == 1L) List(goodReview, badReview)
        else List.empty
      }

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id)
          .someOrFail(throw new NoSuchElementException(s"Review with id $id not found"))
          .map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(throw new NoSuchElementException(s"Review with id $id not found"))

      override def getSummary(companyId: Long): Task[Option[ReviewSummary]]             = ???
      override def insertSummary(companyId: Long, summary: String): Task[ReviewSummary] = ???

    }
  )

  val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 5,
    review = "Great company!",
    created = java.time.Instant.now(),
    updated = java.time.Instant.now()
  )

  val badReview = Review(
    id = 2L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salary = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "Terrible company!",
    created = java.time.Instant.now(),
    updated = java.time.Instant.now()
  )
  override def spec = suite("ReviewServiceSpec")(
    test("create review") {
      for {
        service <- ZIO.service[ReviewService]
        review <- service.create(
          CreateReviewRequest(
            companyId = 1L,
            management = 5,
            culture = 5,
            salary = 5,
            benefits = 5,
            wouldRecommend = 5,
            review = "Great company!"
          ),
          userId = 1L
        )
      } yield assertTrue(review == goodReview)
    },
    test("get by id (found or not)") {
      for {
        service  <- ZIO.service[ReviewService]
        found    <- service.getById(1L)
        notFound <- service.getById(3L)
      } yield assertTrue {
        found == Some(goodReview) && notFound == None
      }
    },
    test("get by companyId") {
      for {
        service       <- ZIO.service[ReviewService]
        fetchedByCid  <- service.getByCompanyId(1L)
        notFoundByCid <- service.getByCompanyId(999L)
      } yield assertTrue {
        fetchedByCid == List(goodReview, badReview) &&
        notFoundByCid == List.empty
      }
    },
    test("get by userId") {
      for {
        service       <- ZIO.service[ReviewService]
        fetchedByUid  <- service.getByUserId(1L)
        notFoundByUid <- service.getByUserId(999L)
      } yield assertTrue {
        fetchedByUid == List(goodReview, badReview) &&
        notFoundByUid == List.empty
      }
    }
  ).provide(
    stubRepoLayer,
    ReviewServiceLive.configuredLayer,
    OpenAiServiceLive.configuredLayer
  )

}
