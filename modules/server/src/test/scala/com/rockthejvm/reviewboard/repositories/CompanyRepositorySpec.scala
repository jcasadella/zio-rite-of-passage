package com.rockthejvm.reviewboard.repositories

import java.sql.SQLException

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import com.rockthejvm.reviewboard.syntax.*

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  val rtjvm = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private def genString() =
    scala.util.Random.alphanumeric.take(8).mkString

  private def genCompany(): Company =
    Company(
      id = -1L,
      slug = genString(),
      name = genString(),
      url = genString(),
      location = Some(genString()),
      country = Some(genString()),
      industry = Some(genString()),
      image = None,
      tags = List(genString(), genString(), genString())
    )

  override val initScript: String = "sql/companies.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          companies <- ZIO.collectAll(
            (1 to 10).map(id => repo.create(rtjvm.copy(id = id.toLong, slug = s"rock-the-jvm-$id")))
          )
          companiesFetched <- repo.getAll
        } yield (companies, companiesFetched)
        program.assert {
          case (companies, companiesFetched) if companies.toSet == companiesFetched.toSet => true
          case _                                                                          => false
        }
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      ),
      test("create a duplicate should fail") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(rtjvm)
          err  <- repo.create(rtjvm).flip
        } yield (err)
        program.assert(_.isInstanceOf[SQLException])
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      ),
      test("get by id and slug") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(rtjvm)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        } yield (fetchedById, fetchedBySlug)
        program.assert {
          case (
                Some(Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _)),
                Some(Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _))
              ) =>
            true
          case _ => false
        }
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      ),
      test("update record") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
          updated <- repo.update(company.id, c => c.copy(url = "blog.rockthejvm.com"))
          fetched <- repo.getById(company.id)
        } yield (company, updated, fetched)
        program.assert {
          case (
                Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _),
                Company(1L, "rock-the-jvm", "Rock the JVM", "blog.rockthejvm.com", _, _, _, _, _),
                Some(
                  Company(1L, "rock-the-jvm", "Rock the JVM", "blog.rockthejvm.com", _, _, _, _, _)
                )
              ) =>
            true
          case _ => false
        }
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      ),
      test("delete record") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
          _       <- repo.delete(company.id)
          fetched <- repo.getById(company.id)
        } yield fetched
        program.assert {
          case None => true
          case _    => false
        }
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      ),
      test("get all") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
          _       <- repo.delete(company.id)
          fetched <- repo.getById(company.id)
        } yield fetched
        program.assert {
          case None => true
          case _    => false
        }
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      ),
      test("search by tag") {
        for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(genCompany())
          fetched <- repo.search(CompanyFilter(tags = List(company.tags.head)))
        } yield assertTrue {
          fetched.nonEmpty && fetched.tail.isEmpty && fetched.head == company
        }
      }.provide(
        CompanyRepositoryLive.layer,
        datasourceLayer,
        Repository.quillLayer
      )
    )
}
