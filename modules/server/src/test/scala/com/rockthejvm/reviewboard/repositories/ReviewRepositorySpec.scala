package com.rockthejvm.reviewboard.repositories

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.syntax.*

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/reviews.sql"

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

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create a review") {
        for {
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield assertTrue {
          review.companyId == 1L &&
          review.userId == 1L &&
          review.management == 5 &&
          review.culture == 5 &&
          review.salary == 5 &&
          review.benefits == 5 &&
          review.wouldRecommend == 5 &&
          review.review == "Great company!"
        }
      }.provide(
        ReviewRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer,
        Scope.default
      ),
      test("get review by ids (id, companyId, userId)") {
        for {
          repo         <- ZIO.service[ReviewRepository]
          created      <- repo.create(goodReview)
          fetchedById  <- repo.getById(created.id)
          fetchedByCid <- repo.getByCompanyId(created.companyId)
          fetchedByUid <- repo.getByUserId(created.userId)
        } yield assertTrue(
          created == goodReview.copy(created = created.created, updated = created.updated) &&
            fetchedById.contains(created) &&
            fetchedByCid.contains(created) &&
            fetchedByUid.contains(created)
        )
      }.provide(
        ReviewRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer,
        Scope.default
      ),
      test("get all") {
        for {
          repo         <- ZIO.service[ReviewRepository]
          review1      <- repo.create(goodReview)
          review2      <- repo.create(badReview)
          fetchedByCid <- repo.getByCompanyId(review1.companyId)
          fetchedByUid <- repo.getByUserId(review1.userId)
        } yield assertTrue(
          review1 == goodReview.copy(created = review1.created, updated = review1.updated) &&
            review2 == badReview.copy(created = review2.created, updated = review2.updated) &&
            fetchedByCid.toSet == Set(review1, review2) &&
            fetchedByUid.toSet == Set(review1, review2)
        )
      }.provide(
        ReviewRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer,
        Scope.default
      ),
      test("edit review") {
        for {
          repo    <- ZIO.service[ReviewRepository]
          review  <- repo.create(goodReview)
          updated <- repo.update(review.id, _.copy(review = "Updated review"))
        } yield assertTrue(
          updated.review == "Updated review" &&
            updated.id == review.id &&
            updated.companyId == review.companyId &&
            updated.userId == review.userId &&
            updated.updated.isAfter(review.updated)
        )
      }.provide(
        ReviewRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer,
        Scope.default
      ),
      test("delete review") {
        for {
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(goodReview)
          _           <- repo.delete(review.id)
          maybeReview <- repo.getById(review.id)
        } yield assertTrue(maybeReview.isEmpty)
      }.provide(
        ReviewRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer,
        Scope.default
      )
    ) @@ TestAspect.sequential
}
