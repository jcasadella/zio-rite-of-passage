package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.domain.errors.UnauthorizedException
import com.rockthejvm.reviewboard.repositories.RecoveryTokensRepository
import com.rockthejvm.reviewboard.repositories.UserRepository

import com.auth0.jwt.exceptions.JWTVerificationException

object UserServiceSpec extends ZIOSpecDefault {

  val daniel = User(
    1L,
    "daniel@rockthejvm.com",
    // Hashed password "mySecretPassword"
    "$argon2id$v=19$m=65536,t=3,p=1$Kf6C5476QLrfQvSVDhBQAWUSBT7x5Zv3SYR85eqasO4z/KdLKnUfkzRbFvFnFvNDbvyhQqWzzOHEPPmdpmKsqw$k3Vn3JkMMLXuNxwyGiXFgmT/H7LKhUFbSzdWNCeLwEc"
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> daniel)
      override def create(user: User): Task[User] = ZIO.succeed {
        db.addOne(user.id, user)
        user
      }
      override def getById(id: Long): Task[Option[User]] =
        ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db.addOne(id, newUser)
        newUser
      }

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db.remove(id)
        user
      }
    }
  }

  val stubTokenRepositoryLayer = ZLayer.succeed {
    new RecoveryTokensRepository {
      val db = collection.mutable.Map[String, String]()
      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).filter(_ == token).nonEmpty)

      override def getToken(email: String): Task[Option[String]] = ZIO.attempt {
        val token = util.Random.alphanumeric.take(8).mkString.toUpperCase()
        db.addOne(email, token)
        Some(token)
      }
    }
  }

  val stubEmailLayer = ZLayer.succeed {
    new EmailService("http://localhost:8080") {
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit
    }
  }

  val stubJwtLayer = ZLayer.succeed {
    new JwtService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.id, user.email, "dummy-token", 60))

      override def verifyToken(token: String): Task[UserId] =
        ZIO.attempt {
          if (token == "dummy-token") UserId(daniel.id, daniel.email)
          else throw new JWTVerificationException("Token not valid")
        }
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(daniel.email, "dummy-password")
          valid   <- service.verifyPassword(daniel.email, "dummy-password")
        } yield assertTrue(valid && user.email == daniel.email)
      },
      test("verify correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "mySecretPassword")
        } yield assertTrue(valid)
      },
      test("verify incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "wrongPassword")
        } yield assertTrue(!valid)
      },
      test("verify non-existent user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someone@gmail.com", "someOtherPassword")
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service  <- ZIO.service[UserService]
          newUser  <- service.updatePassword(daniel.email, "mySecretPassword", "myNewPassword")
          oldValid <- service.verifyPassword(daniel.email, "mySecretPassword")
          newValid <- service.verifyPassword(daniel.email, "myNewPassword")
        } yield assertTrue(!oldValid && newValid)
      },
      test("delete non-existing user should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.delete("someone@gmail.com", "someOtherPassword").flip
        } yield assertTrue(err.isInstanceOf[UnauthorizedException])
      },
      test("delete user with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.delete(daniel.email, "someOtherPassword").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.delete(daniel.email, "mySecretPassword")
        } yield assertTrue(user.email == daniel.email)
      }
    ).provide(
      UserServiceLive.layer,
      HashServiceLiveArgon2.layer,
      stubRepoLayer,
      stubJwtLayer,
      stubEmailLayer,
      stubTokenRepositoryLayer
    )
}
