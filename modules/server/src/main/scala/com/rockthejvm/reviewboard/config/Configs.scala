package com.rockthejvm.reviewboard.config

import com.typesafe.config.ConfigFactory
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.TypesafeConfigProvider

object Configs {
  def makeLayer[C](
      path: String
  )(using desc: DeriveConfig[C], tag: Tag[C]): ZLayer[Any, Throwable, C] = {
    // Convert path with dots to a nested Config[C]
    val nestedConfig = path.split('.').foldRight(deriveConfig[C])((node, acc) => acc.nested(node))
    ZLayer
      .fromZIO(
        ZIO
          .config(nestedConfig)
          // Force reading properties file
          .withConfigProvider(TypesafeConfigProvider.fromResourcePath())
      )
  }
}
