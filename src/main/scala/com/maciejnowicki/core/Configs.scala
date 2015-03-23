package com.maciejnowicki.core

import com.typesafe.config.ConfigFactory

object Configs {

  val config = ConfigFactory.load()

  val port = config.getInt("app.port")

  val mongoDBHost = config.getString("mongo.host")
  val mongoDBName = config.getString("mongo.db")

}
