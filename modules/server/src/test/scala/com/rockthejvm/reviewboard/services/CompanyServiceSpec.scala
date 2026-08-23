package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import com.rockthejvm.reviewboard.syntax.*

object CompanyServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CompanyService]

  val stubRepoLayer = ZLayer.succeed(
    new CompanyRepository {
      val db = collection.mutable.Map[Long, Company]()
      override def create(company: Company): Task[Company] =
        ZIO.succeed {
          val nextId     = db.keys.maxOption.getOrElse(0L) + 1
          val newCompany = company.copy(id = nextId)
          db.addOne(nextId -> newCompany)
          newCompany
        }
      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val company        = db(id)
          val updatedCompany = op(company)
          db.update(id, updatedCompany)
          updatedCompany
        }

      override def delete(id: Long): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db.remove(id)
          company
        }

      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def getAll: Task[List[Company]] =
        ZIO.succeed(db.values.toList)
    }
  )

  val companyReq       = CreateCompanyRequest("Rock the JVM", "rockthejvm.com")
  val companyGoogleReq = CreateCompanyRequest("Google", "google.com")
  override def spec: Spec[TestEnvironment, Any] = suite("CompanyServiceSpec")(
    test("create company") {
      val companyZIO = service(_.create(companyReq))

      companyZIO.assert { company =>
        company.name == "Rock the JVM" &&
        company.url == "rockthejvm.com" &&
        company.slug == "rock-the-jvm"
      }
    },
    test("get by id") {
      val program = for {
        company    <- service(_.create(companyReq))
        companyOpt <- service(_.getById(company.id))
      } yield (company, companyOpt)

      program.assert {
        case (company, Some(companyRetrieved)) =>
          companyRetrieved.name == "Rock the JVM" &&
          companyRetrieved.url == "rockthejvm.com" &&
          companyRetrieved.slug == "rock-the-jvm" &&
          company == companyRetrieved
        case _ => false
      }
    },
    test("get by slug") {
      val program = for {
        company    <- service(_.create(companyReq))
        companyOpt <- service(_.getBySlug(company.slug))
      } yield (company, companyOpt)

      program.assert {
        case (company, Some(companyRetrieved)) =>
          companyRetrieved.name == "Rock the JVM" &&
          companyRetrieved.url == "rockthejvm.com" &&
          companyRetrieved.slug == "rock-the-jvm" &&
          company == companyRetrieved
        case _ => false
      }
    },
    test("get all") {
      val program = for {
        company       <- service(_.create(companyReq))
        companyGoogle <- service(_.create(companyGoogleReq))
        companies     <- service(_.getAll)
      } yield (company, companyGoogle, companies)

      program.assert {
        case (company, companyGoogle, companies) =>
          companies.toSet == Set(company, companyGoogle)
        case _ => false
      }

    }
  ).provide(CompanyServiceLive.layer, stubRepoLayer)
}
