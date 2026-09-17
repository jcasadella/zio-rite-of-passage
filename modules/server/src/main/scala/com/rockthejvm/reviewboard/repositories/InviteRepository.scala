package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.InviteNamedRecord
import com.rockthejvm.reviewboard.domain.data.InviteRecord
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait InviteRepository {
  def getByUserEmail(userEmail: String): Task[List[InviteNamedRecord]]
  def getInvitePack(userEmail: String, companyId: Long): Task[Option[InviteRecord]]
  def addInvitePack(userEmail: String, companyId: Long, nInvites: Int): Task[Long]
  def activatePack(id: Long): Task[Boolean]
  def markInvites(userEmail: String, companyId: Long, nInvites: Int): Task[Int]
}

class InviteRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends InviteRepository {
  import quill.*
  inline given schema: SchemaMeta[InviteRecord] = schemaMeta[InviteRecord]("invites")
  inline given insMeta: InsertMeta[InviteRecord] = insertMeta[InviteRecord](_.id)
  inline given updMeta: UpdateMeta[InviteRecord] = updateMeta[InviteRecord](_.id)

  inline given companySchema: SchemaMeta[Company] = schemaMeta[Company]("companies")
  inline given companyInsMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given companyUpdMeta: UpdateMeta[Company] = updateMeta[Company](_.id)

  override def getByUserEmail(userEmail: String): Task[List[InviteNamedRecord]] =
    run(
      for {
        (record, company) <- query[InviteRecord]
                               .join(query[Company])
                               .on((inv, cmp) => inv.companyId == cmp.id)
        if record.userEmail == lift(userEmail)
        if record.nInvites > 0
        if record.active
      } yield InviteNamedRecord(company.id, company.name, record.nInvites)
    )

  override def getInvitePack(userEmail: String, companyId: Long): Task[Option[InviteRecord]] =
    run(
      query[InviteRecord]
        .filter(_.companyId == lift(companyId))
        .filter(_.userEmail == lift(userEmail))
        .filter(_.active)
    ).map(_.headOption)

  override def addInvitePack(userEmail: String, companyId: Long, nInvites: Int): Task[Long] =
    run(
      query[InviteRecord]
        .insertValue(lift(InviteRecord(-1, userEmail, companyId, nInvites, false)))
        .returning(_.id)
    )

  /**
    * 
    * @param id InviteRecord's id
    * @return true if found a row with that id and set active = true, else otherwise
    */
  override def activatePack(id: Long): Task[Boolean] =
    run(
      query[InviteRecord]
        .filter(_.id == lift(id))
        .update(_.active -> lift(true))
    ).map(updatedRows => updatedRows > 0)

  override def markInvites(userEmail: String, companyId: Long, nInvites: Int): Task[Int] =
    for {
      currentRecord <- getInvitePack(userEmail, companyId)
                         .someOrFail(new RuntimeException(s"User $userEmail cannot send invites for company id $companyId"))
      nInvitesMarked <- ZIO.succeed(Math.min(nInvites, currentRecord.nInvites))
      _ <- run(
             query[InviteRecord]
               .filter(_.id == lift(currentRecord.id))
               .updateValue(lift(currentRecord.copy(nInvites = currentRecord.nInvites - nInvitesMarked)))
               .returning(r => r)
      )
    } yield nInvitesMarked
}

object InviteRepositoryLive {
  val layer = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield new InviteRepositoryLive(quill)
  }
}

object InviteRepositoryDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    val program = for {
      repo <- ZIO.service[InviteRepository]
      records <- repo.getByUserEmail("m@rockthejvm.com")
      _ <- Console.printLine(s"Records: ${records}")
    } yield ()
    program.provide(
      InviteRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}