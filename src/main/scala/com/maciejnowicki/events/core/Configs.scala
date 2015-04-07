package com.maciejnowicki.events.core

import com.typesafe.config.{ConfigException, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

object Configs extends StrictLogging{

  val config = ConfigFactory.load()

  val port = config.getInt("app.port")

  val mongoDBHost = config.getString("mongo.host")

  val mongoDBName = config.getString("mongo.db")

  val mongoUri = try {
    Some(config.getString("mongo.mongoUri"))
  } catch {
    case e: ConfigException.Missing => {
      logger.info("MONGOLAB_URI does not exist")
      None
    }
   }


  val eventPingTolerance = config.getInt("events.pingTolerance")




}
