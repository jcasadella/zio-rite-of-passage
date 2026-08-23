package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Company
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait CompanyRepository {
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def getAll: Task[List[Company]]
}

class CompanyRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {
  import quill.*
  inline given schema: SchemaMeta[Company] = schemaMeta[Company]("companies")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given updMeta: UpdateMeta[Company] = updateMeta[Company](_.id)
  

  override def create(company: Company): Task[Company] =
    run{
      query[Company]
        .insertValue(lift(company))
        .returning(res => res)
    }
  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(throw new NoSuchElementException(s"Company with id $id not found"))
      updated <- run {
        query[Company]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(res => res)
      }
    } yield updated
  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(res => res)
    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company]
        .filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company]
        .filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def getAll: Task[List[Company]] =
    run {
      query[Company]
    }

}

object CompanyRepositoryLive {
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, CompanyRepository] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(new CompanyRepositoryLive(_))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {

  val program = for {
      repo <- ZIO.service[CompanyRepository]
      _ <- repo.create(Company(0, "Rock the JVM", "rockthejvm", "https://rockthejvm.com"))
    } yield ()
  
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    program.provide(
      CompanyRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("rockthejvm.db")
    )

}