package com.maciejnowicki.models

import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions


import scala.concurrent.{Future, Await}
import scala.concurrent.duration._


class DateTimeEventTest extends Specification with NoTimeConversions {
  sequential

  val providerName = "testProvider";

  val name = DateTimeEvent.getClass.getSimpleName

  val fromDate = DateTime.now();
  val toDate = fromDate.plusHours(2).plusMinutes(15)

  val exampleEvent = DateTimeEvent("0", providerName, "testUser", fromDate, toDate)

  "Test DateTimeEventTest interaction with database" in {

    s"should insert, update, retrieve and remove $name from database" in {
      println("Start 1")

      removeAllTestElements()

      val toInsert = exampleEvent;

      val update = Await.result(DateTimeEvent.insertUpdate(toInsert), 1 seconds)

      update.isDefined must beTrue


      val elem = {
        val opt = Await.result(DateTimeEvent.getAnyElementByProvider(providerName), 5 seconds)

        opt.isDefined must beTrue

        opt.get
      }

      val changedElem = elem.copy(user = elem.user + " Updated")

      val updated = Await.result(DateTimeEvent.insertUpdate(changedElem), 5 seconds)

      updated.isDefined must beFalse

      val retrieved = Await.result(DateTimeEvent.get(changedElem.id), 5 seconds)

      retrieved.isDefined must beTrue

      println("Ending 1")

      retrieved.get must beEqualTo(changedElem)


    }

    "getLatestByFromDate should get lastest element by fromDate field" in {
      println("Start 2")

      removeAllTestElements();

      val minusSeconds = exampleEvent.copy(from = exampleEvent.from.minusSeconds(1))

      val toInsert = List(
        minusSeconds,
        exampleEvent.copy(from = exampleEvent.from.minusMinutes(1)),
        exampleEvent.copy(from = exampleEvent.from.minusHours(1)),
        exampleEvent.copy(from = exampleEvent.from.minusDays(1)),
        exampleEvent.copy(from = exampleEvent.from.minusYears(1))
      )

      val inserted = Await.result(Future.sequence(toInsert.map(DateTimeEvent.insertUpdate(_))), 30 seconds)

      inserted.foreach(_.isDefined must beTrue)

      val minusSecondsWithNewId = minusSeconds.copy(id = inserted.head.get)

      val latest = Await.result(DateTimeEvent.getLatestByFromDate(providerName, exampleEvent.user), 5 seconds)

      latest.isDefined must beTrue

      println("Ending 2")

      latest.get must beEqualTo(minusSecondsWithNewId)

    }

    "removeByDate should work" in {
      sequential
      println("Start 3")


      val date = exampleEvent.from

      val beforeDateEvents =   List(
        exampleEvent.copy(from = exampleEvent.from.minusMinutes(1)),
        exampleEvent.copy(from = exampleEvent.from.minusHours(1)),
        exampleEvent.copy(from = exampleEvent.from.minusDays(1)),
        exampleEvent.copy(from = exampleEvent.from.minusYears(1))
      )

      val afterDateEvents = List(
        exampleEvent.copy(from = exampleEvent.from.plusMinutes(1)),
        exampleEvent.copy(from = exampleEvent.from.plusHours(1)),
        exampleEvent.copy(from = exampleEvent.from.plusDays(1)),
        exampleEvent.copy(from = exampleEvent.from.plusYears(1))
      )


      "getyDate only with from argument should work" in {

        removeAllTestElements()


        Await.result(Future.sequence(beforeDateEvents map DateTimeEvent.insertUpdate), 10 seconds)

        val newIndexes = Await.result(Future.sequence(afterDateEvents map DateTimeEvent.insertUpdate), 10 seconds)

        newIndexes foreach(_.isDefined must beTrue)

        val afterDateEventsWithNewIndexes = afterDateEvents.zip(newIndexes).map(x => x._1.copy(id = x._2.get))

        val result = Await.result(DateTimeEvent.getByDate(Some(date)), 30 seconds)


        result.toSet must beEqualTo(afterDateEventsWithNewIndexes.toSet)
      }



/*      "getByDate with user specified should work " in {


        val (left, right) = afterDateEvents.splitAt(2)




      }*/





    }

  }


  private def removeAllTestElements(): Unit = {
    //remove existing elements
    val map = DateTimeEvent.deleteAllByProvider(providerName).map{
      n => println("Deleted " + n +" elements")
    }
    Await.result(map, 5 seconds)
  }
}
