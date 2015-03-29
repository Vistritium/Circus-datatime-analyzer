package com.maciejnowicki.core

import spray.routing.HttpService

trait ResourcesService extends HttpService {

  val resourceRoute =
    pathPrefix("res") {
      println("res matched")
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
          println("Matched events, provider: " + provider)
          getFromResource("public/views/events.html")
        }
      }
    }


  def getFile(path: String) = {
    println(path)

  }

}
