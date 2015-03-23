package com.maciejnowicki.core

import reactivemongo.api.MongoDriver

import scala.concurrent.ExecutionContext.Implicits.global

object MongoDB {

  // gets an instance of the driver
  // (creates an actor system)
  private[this] val driver = new MongoDriver
  private[this] val connection = driver.connection(List(Configs.mongoDBHost))

  // Gets a reference to the database "plugin"
  val db = connection(Configs.mongoDBName)

  def init(): Unit ={
    //initializes this object..
  }


}
