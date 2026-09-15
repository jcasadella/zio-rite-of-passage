package com.rockthejvm.reviewboard.services

import cats.kernel.instances.hash
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.domain.errors.UnauthorizedException
import com.rockthejvm.reviewboard.repositories.RecoveryTokensRepository
import com.rockthejvm.reviewboard.repositories.UserRepository
import zio.*

import java.security.SecureRandom

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def delete(email: String, password: String): Task[User]
  // JWT
  def generateToken(email: String, password: String): Task[Option[UserToken]]
  // password recovery flow
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
  userRepository: UserRepository,
  hashService: HashService,
  jwtService: JwtService,
  emailService: EmailService,
  tokenRepository: RecoveryTokensRepository)
  extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    for {
      hashedPassword <- hashService.hash(password)
      user <- userRepository.create(User(0L, email, hashedPassword))
    } yield user

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      maybeUser <- userRepository.getByEmail(email)
      result <- maybeUser match {
        case Some(existingUser) => hashService.verify(password, existingUser.hashedPassword)
                                              .orElseSucceed(false)
        case None => ZIO.succeed(false)
      }
    } yield result

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- userRepository
        .getByEmail(email)
        .someOrFail(UnauthorizedException(s"User with email $email not found"))
      verified <- hashService.verify(password, existingUser.hashedPassword)
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken


  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- userRepository.getByEmail(email)
      .someOrFail(UnauthorizedException(s"User with email $email not found"))
      verified <- hashService.verify(oldPassword, existingUser.hashedPassword)
      newHash <- hashService.hash(newPassword)
      updatedUser <- userRepository.update(
        existingUser.id,
        user => user.copy(hashedPassword = newHash)
      ).when(verified)
      .someOrFail(new RuntimeException(s"Could not update password for $email"))
    } yield updatedUser

  override def delete(email: String, password: String): Task[User] =
    for {
      existingUser <- userRepository.getByEmail(email)
      .someOrFail(UnauthorizedException(s"User with email $email not found"))
      verified <- hashService.verify(password, existingUser.hashedPassword)
      updatedUser <- userRepository.delete(existingUser.id).when(verified)
      .someOrFail(new RuntimeException(s"Could not delete user $email"))
    } yield updatedUser

  override def sendPasswordRecoveryToken(email: String): Task[Unit] =
    tokenRepository.getToken(email).flatMap {
      case Some(token) => emailService.sendPasswordRecoveryEmail(email, token)
      case None => ZIO.unit
    }
  
  override def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean] = for {
    existingUser <- userRepository.getByEmail(email).someOrFail(new RuntimeException("Non-existent user"))
    tokenIsValid <- tokenRepository.checkToken(email, token)
    newHashedPassword <- hashService.hash(newPassword)
    result <- userRepository
      .update(existingUser.id, user => user.copy(hashedPassword = newHashedPassword))
      .when(tokenIsValid)
      .map(_.nonEmpty)
  } yield result

}

object UserServiceLive {
  val layer = ZLayer {
    for {
      userRepository <- ZIO.service[UserRepository]
      hashService <- ZIO.service[HashService]
      jwtService <- ZIO.service[JwtService]
      emailService <- ZIO.service[EmailService]
      tokenRepository <- ZIO.service[RecoveryTokensRepository]
    } yield new UserServiceLive(userRepository, hashService, jwtService, emailService, tokenRepository)
  }
}
