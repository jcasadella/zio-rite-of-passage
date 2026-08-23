package com.rockthejvm.reviewboard.services

import zio.*

import java.security.SecureRandom

trait OtpGenerator {
  def generateToken: UIO[String]
}

final class OtpGeneratorLive private extends OtpGenerator {
  // Removed lookalike characters (0, O, 1, I) for better UX
  private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
  private val length = 8

  // SecureRandom is thread-safe, so we can reuse a single instance
  private val secureRandom = new SecureRandom()

  override def generateToken: UIO[String] = ZIO.succeed {
    (1 to length)
    .map(_ => alphabet(secureRandom.nextInt(alphabet.length)))
    .mkString
  }
}

object OtpGeneratorLive {
  val layer = ZLayer.succeed(new OtpGeneratorLive)
}
