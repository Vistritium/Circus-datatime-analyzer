package com.maciejnowicki.core

import com.maciejnowicki.DBTestUtils
import com.maciejnowicki.models.DateTimeEvent
import org.specs2.mutable.Specification
import spray.json._
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import org.joda.time.DateTime
import AppJsonProtocol._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller

class AppServicesSpec extends Specification with Specs2RouteTest with DateTimeEventService {
  sequential
  def actorRefFactory = system

  val providerName = "testProvider";

  "DateTimeEventService should work" in {

    "updating should work" in {
      DBTestUtils.removeAllTestElements()
      val jsVal = JsObject(
        "appear" -> DateTime.now.minusHours(3).toJson,
        "disappear" -> DateTime.now.toJson,
        "provider" -> JsString(providerName),
        "user" -> JsString("TestUser")
      )

      Post("/updateEvent", jsVal) ~> dateTimeEventRoute ~> check {
        val value = responseAs[String]

        println(value)
      }
      ok

    }
    "pinging should work" in {
      sequential

      val jsVal = JsObject(
      "provider" -> JsString(providerName),
      "user" -> JsString("randomUser")
      )

      DBTestUtils.removeAllTestElements()
      Post("/pingEvent", jsVal) ~> dateTimeEventRoute ~> check {
        val value = responseAs[String]
        println("pingEvent: " + value)


        Post("/pingEvent", jsVal) ~> dateTimeEventRoute ~> check {
          val value = responseAs[String]
          println("pingEvent#2: " + value)
        }

      }


      DBTestUtils.removeAllTestElements()
      Post("/pingEvents", JsArray(jsVal, jsVal)) ~> dateTimeEventRoute ~> check {
        val value = responseAs[String]

        println("pingEvents: " + value)
      }

      ok
    }

/*    "leave GET requests to other paths unhandled" >> {
      Get("/kermit") ~> resourceRoute ~> check {
        handled must beFalse
      }
    }*/

/*    "return a MethodNotAllowed error for PUT requests to the root path" >> {
      Put() ~> sealRoute(resourceRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }*/
  }
}
