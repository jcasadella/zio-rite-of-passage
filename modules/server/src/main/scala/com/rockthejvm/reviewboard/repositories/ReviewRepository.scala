package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.domain.data.ReviewSummary
import io.getquill.*
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

import java.time.Instant

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
  def getSummary(companyId: Long): Task[Option[ReviewSummary]]
  def insertSummary(companyId: Long, summary: String): Task[ReviewSummary]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*

  inline given reviewSchema: SchemaMeta[Review] = schemaMeta[Review]("reviews")
  inline given reviewInsMeta: InsertMeta[Review] = insertMeta[Review](_.id, _.created, _.updated)
  inline given reviewUpdMeta: UpdateMeta[Review] = updateMeta[Review](_.id, _.companyId, _.userId, _.created)

  inline given reviewSummarySchema: SchemaMeta[ReviewSummary] = schemaMeta[ReviewSummary]("review_summaries")
  inline given reviewSummaryInsMeta: InsertMeta[ReviewSummary] = insertMeta[ReviewSummary]()
  inline given reviewSummaryUpdMeta: UpdateMeta[ReviewSummary] = updateMeta[ReviewSummary]()

  override def create(review: Review): Task[Review] =
    run(query[Review].insertValue(lift(review)).returning(r => r))

  override def getById(id: Long): Task[Option[Review]] =
    run(query[Review].filter(_.id == lift(id))).map(_.headOption)

  override def getByUserId(userId: Long): Task[List[Review]] =
    run(query[Review].filter(_.userId == lift(userId)))

  override def getByCompanyId(companyId: Long): Task[List[Review]] =
    run(query[Review].filter(_.companyId == lift(companyId)))

  override def update(id: Long, op: Review => Review): Task[Review] = for {
    current <- getById(id).someOrFail(throw new NoSuchElementException(s"Review with id $id not found"))
    updated <- run(query[Review].filter(_.id == lift(id)).updateValue(lift(op(current))).returning(res => res))
  } yield updated

  override def delete(id: Long): Task[Review] =
    run(query[Review].filter(_.id == lift(id)).delete.returning(res => res))

  override def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
    run(query[ReviewSummary].filter(_.companyId == lift(companyId))).map(_.headOption)

  override def insertSummary(companyId: Long, summary: String): Task[ReviewSummary] =
    run {
      query[ReviewSummary]
        .insertValue(lift(ReviewSummary(companyId, summary, Instant.now())))
        .onConflictUpdate(_.companyId)(
          (existing, excluded) => existing.contents -> excluded.contents,
          (existing, excluded) => existing.created -> excluded.created
        )
        .returning(result => result)
    }
}

object ReviewRepositoryLive {
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepository] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(new ReviewRepositoryLive(_))
  }
}

object ReviewRepositoryPlayground extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    val program = for {
      repo <- ZIO.service[ReviewRepository]
      _ <- repo.insertSummary(1, "This is a first summary")
      summary <- repo.getSummary(1)
      _ <- Console.printLine(summary)
      _ <- repo.insertSummary(1, "This is a second summary")
      summary2 <- repo.getSummary(1)
      _ <- Console.printLine(summary2)
    } yield ()
    program.provide(ReviewRepositoryLive.layer, Repository.dataLayer)
  }
}