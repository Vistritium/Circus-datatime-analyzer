package com.maciejnowicki.core

import spray.routing.HttpService

trait ResourcesService extends HttpService {

  val resourceRoute =
  pathPrefix("res") {
    println("res matched")
    get {
      unmatchedPath{
        remaining => {
          getFromResource("public/" + remaining)
        }
      }
    }
  } ~ path(""){
    getFromResource("public/views/main.html")
  }



  def getFile(path: String) = {
      println(path)

  }

}
