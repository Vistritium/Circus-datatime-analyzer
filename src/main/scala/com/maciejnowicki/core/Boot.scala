package com.maciejnowicki.core

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, _}
import spray.can.Http
import reactivemongo.api._
import reactivemongo.bson._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("circus-datatime-analyzer")

  // create and start our service actor
  val service = system.actorOf(Props[MyServiceActor], "mainService")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = Configs.port)

  MongoDB.init()

  val query = BSONDocument("name" -> "Maciej")

  val collection = MongoDB.db[BSONCollection]("test")
  val list = Await.result(collection.find(query).cursor[BSONDocument].collect[List](), 5 seconds)
  list.foreach(x => {
    BSONDocument.pretty(x)
  })


  def doSomething(): Unit ={





  }


}
