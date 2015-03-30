package com.maciejnowicki.core

import com.typesafe.scalalogging.StrictLogging
import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.concurrent.ExecutionContext.Implicits.global

object MongoDB extends StrictLogging{

  // gets an instance of the driver
  // (creates an actor system)
  private[this] val driver = new MongoDriver
  private[this] val connection = Configs.mongoUri match {
    case Some(x) => {
      logger.info("Got mongo uri: " + x)
      driver.connection(MongoConnection.parseURI(x).get)
    }
    case None => driver.connection(List(Configs.mongoDBHost))
  }



  // Gets a reference to the database "plugin"
  val db = connection(Configs.mongoUri match {
    case Some(x) => MongoConnection.parseURI(x).get.db.getOrElse(Configs.mongoDBName)
    case None => Configs.mongoDBName
  })

    //connection(Configs.mongoDBName)

  def init(): Unit ={
    //initializes this object..
  }


}
