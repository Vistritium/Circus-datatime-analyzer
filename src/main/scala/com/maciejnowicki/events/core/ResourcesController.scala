package com.maciejnowicki.events.core

import com.typesafe.scalalogging.StrictLogging
import spray.routing.HttpService

trait ResourcesController extends HttpService with StrictLogging {

  val resourceRoute =
    pathPrefix("res") {
      get {
        unmatchedPath {
          remaining => {
            getFromResource("public/" + remaining)
          }
        }
      }
    } ~ path("") {
      get {
        getFromResource("public/views/main.html")
      }
    } ~ path("events" / Segment) {
      provider => {
        get {
          logger.info("Matched events, provider: " + provider)
          getFromResource("public/views/events.html")
        }
      }
    }

}
