package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
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
  def uniqueAttribues: Task[CompanyFilter]
  def search(filter: CompanyFilter): Task[List[Company]]
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

  override def uniqueAttribues: Task[CompanyFilter] =
    for {
      locations <- run(query[Company].filter(_.location.isDefined).map(c => sql"${c.location}".as[String]).distinct) // This creates a subquery that Postgres should be capable to simplify
      countries <- run(query[Company].filter(_.country.isDefined).map(c => sql"DISTINCT ${c.country}".as[String])) // DISTINCT NOT NULL <- most perfromance option
      industries <- run(query[Company].map(_.industry).distinct).map(_.flatMap(_.toList)) // Get all in memory and traverse it using Scala
      tags <- run(query[Company].concatMap(_.tags).distinct) // Using UNNEST oprator (for the TEXT[] type) and a subquery Postgres is capable to simplify in the planning
    } yield CompanyFilter(locations, countries, industries, tags)
  
  override def search(filter: CompanyFilter): Task[List[Company]] =
    if (filter.isEmpty) getAll
    else
      run {
        query[Company]
        .filter { company =>
          liftQuery(filter.locations.toSet).contains(company.location) ||
          liftQuery(filter.countries.toSet).contains(company.country) ||
          liftQuery(filter.industries.toSet).contains(company.industry) ||
          sql"${company.tags} && ${lift(filter.tags)}".as[Boolean] // Operator && is the most performant to check inside a TEXT[]
        }
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