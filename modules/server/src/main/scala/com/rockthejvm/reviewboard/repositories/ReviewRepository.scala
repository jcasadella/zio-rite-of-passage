package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import io.getquill.*
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*

  inline given reviewSchema: SchemaMeta[Review] = schemaMeta[Review]("reviews")
  inline given reviewInsMeta: InsertMeta[Review] = insertMeta[Review](_.id, _.created, _.updated)
  inline given reviewUpdMeta: UpdateMeta[Review] = updateMeta[Review](_.id, _.companyId, _.userId, _.created)

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
}

object ReviewRepositoryLive {
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepository] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(new ReviewRepositoryLive(_))
  }
}
