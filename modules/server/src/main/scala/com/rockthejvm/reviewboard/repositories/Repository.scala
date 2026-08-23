package com.rockthejvm.reviewboard.repositories

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

object Repository {
  def quillLayer =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataSourceLayer =
    Quill.DataSource.fromPrefix("rockthejvm.db")

  val dataLayer = dataSourceLayer >>> quillLayer
}
