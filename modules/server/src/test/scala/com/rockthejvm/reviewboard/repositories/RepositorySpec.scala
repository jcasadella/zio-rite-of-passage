package com.rockthejvm.reviewboard.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.*

import javax.sql.DataSource

trait RepositorySpec {

  val initScript: String
  
  def createContainer() = {
    val container: PostgreSQLContainer =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine")).withInitScript(
        initScript
      )

    container.start()
    container
  }

  def createDatasource(container: PostgreSQLContainer): DataSource = {
    val datasource = new PGSimpleDataSource()
    datasource.setUrl(container.getJdbcUrl())
    datasource.setUser(container.getUsername())
    datasource.setPassword(container.getPassword())
    datasource
  }

  val datasourceLayer = ZLayer.scoped {
    for {
      container <- ZIO.acquireRelease(ZIO.attemptBlocking(createContainer()))(container =>
        ZIO.attemptBlocking{
          container.stop()
        }.ignoreLogged
      )
      datasource <- ZIO.attempt(createDatasource(container))
    } yield (datasource)
  }
}
