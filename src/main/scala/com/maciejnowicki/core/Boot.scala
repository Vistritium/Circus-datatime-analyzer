package com.maciejnowicki.core

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App with StrictLogging{


  implicit val system = ActorSystem("circus-datatime-analyzer")

  val service = system.actorOf(Props[AppControllers], "mainService")

  implicit val timeout = Timeout(5.seconds)

  logger.info("binding to " + Configs.port)
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = Configs.port)

  MongoDB.init()


  def doSomething(): Unit ={





  }


}
