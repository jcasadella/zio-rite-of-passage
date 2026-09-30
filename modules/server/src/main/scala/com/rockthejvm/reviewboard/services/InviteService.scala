package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.InvitePackConfig
import com.rockthejvm.reviewboard.domain.data.InviteNamedRecord
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import com.rockthejvm.reviewboard.repositories.InviteRepository
import zio.*

trait InviteService {
  def getByUserId(userEmail: String): Task[List[InviteNamedRecord]]
  def sendInvites(userEmail: String, companyId: Long, receivers: List[String]): Task[Int]
  def addInvitePack(userEmail: String, companyId: Long): Task[Long]
  def activatePack(id: Long): Task[Boolean]
}

class InviteServiceLive private (
  inviteRepository: InviteRepository,
  companyRepository: CompanyRepository,
  emailService: EmailService,
  config: InvitePackConfig
) extends InviteService {

  override def getByUserId(userEmail: String): Task[List[InviteNamedRecord]] =
    inviteRepository.getByUserEmail(userEmail)

  // invariant: only one pack per user per company
  override def addInvitePack(userEmail: String, companyId: Long): Task[Long] =
    for {
      company <- companyRepository.getById(companyId)
                    .someOrFail(new RuntimeException(s"Cannot invite to review: company $companyId doesn't exist"))
      currentPack <- inviteRepository.getInvitePack(userEmail, companyId)
      newPackId <- currentPack match {
        case None => inviteRepository.addInvitePack(userEmail, companyId, config.nInvites)
        case Some(_) => ZIO.fail(new RuntimeException("You already have an active pack for this company"))
      }
    } yield newPackId

  override def sendInvites(userEmail: String, companyId: Long, receivers: List[String]): Task[Int] =
    for {
      company <- companyRepository.getById(companyId)
                   .someOrFail(new RuntimeException(s"Cannot send invites: company $companyId doesn't exist"))
      nInvitesMarked <- inviteRepository.markInvites(userEmail, companyId, receivers.size)
      _ <- ZIO.collectAllPar(
             receivers
               .take(nInvitesMarked)
               .map(recevier => emailService.sendReviewInvite(userEmail, recevier, company))
           )
    } yield nInvitesMarked

  override def activatePack(id: Long): Task[Boolean] =
    inviteRepository.activatePack(id)
}

object InviteServiceLive {
  val layer = ZLayer {
    for {
      inviteRepository <- ZIO.service[InviteRepository]
      companyRepository <- ZIO.service[CompanyRepository]
      emailService <- ZIO.service[EmailService]
      invitePackConfig <- ZIO.service[InvitePackConfig]
    } yield InviteServiceLive(inviteRepository, companyRepository, emailService, invitePackConfig)
  }

  val configuredLayer =
    Configs.makeLayer[InvitePackConfig]("rockthejvm.invites") >>> layer
}
