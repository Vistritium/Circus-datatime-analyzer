package com.maciejnowicki.core

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
  def actorRefFactory = system

  "MyService" in {

    "return a greeting for GET requests to the root path" in {

      val jsVal = JsObject(
        "appear" -> DateTime.now.minusHours(3).toJson,
        "disappear" -> DateTime.now.toJson,
        "provider" -> JsString("testProvider"),
        "user" -> JsString("TestUser")
      )

      Post("/updateEvent", jsVal) ~> dateTimeEventRoute ~> check {
        val value = responseAs[String]

        println(value)


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
