package com.maciejnowicki

import com.maciejnowicki.models.DateTimeEvent
import com.typesafe.scalalogging.{StrictLogging, LazyLogging}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await

object DBTestUtils extends StrictLogging {

  val testProvider = "testProvider"

  def removeAllTestElements(): Unit = {



    //remove existing elements
    val map = DateTimeEvent.deleteAllByProvider(testProvider).map{
      n => logger.info("Deleted " + n +" elements")
    }
    Await.result(map, 5 seconds)
  }
}
