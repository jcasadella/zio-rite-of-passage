package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.FlywayConfig
import org.flywaydb.core.Flyway
import zio.*

trait FlywayService {
  def clean: Task[Unit]
  def runBaseline: Task[Unit]
  def runMigrations: Task[Unit]
  def runRepairs: Task[Unit]
}

class FlywayServiceLive private (flyway: Flyway) extends FlywayService {

  override def clean: Task[Unit] =
    ZIO.attemptBlocking(flyway.clean())

  override def runBaseline: Task[Unit] =
    ZIO.attemptBlocking(flyway.baseline())

  override def runMigrations: Task[Unit] =
    ZIO.attemptBlocking(flyway.migrate())

  override def runRepairs: Task[Unit] =
    ZIO.attemptBlocking(flyway.repair())
  
}

object FlywayServiceLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[FlywayConfig]
      flyway <- ZIO.attempt(
        Flyway
          .configure()
          .dataSource(config.url, config.user, config.password)
          .load()
      )
    } yield new FlywayServiceLive(flyway)
  }

  val configuredLayer =
    Configs.makeLayer[FlywayConfig]("rockthejvm.db.dataSource") >>> layer
}
