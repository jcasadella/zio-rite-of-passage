package com.rockthejvm.reviewboard.services

import com.password4j.AlgorithmFinder
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import zio.*

trait HashService {
  def hash(plainText: String): Task[String]
  def verify(plainText: String, hashed: String): Task[Boolean]
}

class HashServiceLiveArgon2 private (argon2: Argon2Function) extends HashService {
  
  override def hash(plainText: String): Task[String] =
    ZIO.attempt {
      Password.hash(plainText)
      .addRandomSalt()
      .`with`(argon2)
      .getResult
    }
  
  override def verify(plainText: String, hashed: String): Task[Boolean] =
    ZIO.attempt {
      Password.check(plainText, hashed)
      .`with`(argon2)
    }
}

object HashServiceLiveArgon2 {
  // These are standard OWASP recommended minimums for Argon2id.
  private val argon2Config = Argon2Function.getInstance(
    65536,                           // Memory in KB (64 MB)
    3,                               // Iterations (Time cost)
    1,                               // Parallelism (Threads)
    32,                              // Output hash length in bytes
    Argon2.ID,                       // Argon2id variant
    Argon2Function.ARGON2_VERSION_13 // Modern version
  )
  val layer = ZLayer.succeed(HashServiceLiveArgon2(argon2Config))
}

object HashServiceDemo extends ZIOAppDefault {

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    val hashServiceLayer = HashServiceLiveArgon2.layer
    val program = for {
      hashService <- ZIO.service[HashService]
      hashed <- hashService.hash("mySecretPassword")
      isValid <- hashService.verify("mySecretPassword", hashed)
      _ <- Console.printLine(s"Hashed password: $hashed")
      _ <- Console.printLine(s"Is valid: $isValid")
    } yield ()
    program.provideLayer(hashServiceLayer)
  }
}