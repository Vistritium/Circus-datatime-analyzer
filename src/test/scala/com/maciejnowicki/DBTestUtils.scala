package com.maciejnowicki

import com.maciejnowicki.models.DateTimeEvent
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await

object DBTestUtils {

  val testProvider = "testProvider"

  def removeAllTestElements(): Unit = {



    //remove existing elements
    val map = DateTimeEvent.deleteAllByProvider(testProvider).map{
      n => println("Deleted " + n +" elements")
    }
    Await.result(map, 5 seconds)
  }
}
