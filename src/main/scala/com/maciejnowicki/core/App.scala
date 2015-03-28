package com.maciejnowicki.core

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.http.StatusCodes
import spray.routing._
import StatusCodes._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class App extends Actor with ResourcesService with DateTimeEventService{

/*  implicit val myRejectionHandler = RejectionHandler {
    case MissingCookieRejection(cookieName) :: _ =>
      complete(BadRequest, "No cookies, no service!!!")
    case MethodRejection(supported) :: _ => {
      complete(NotFound, "ojojoj " + supported.toString())
    }
  }*/

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(resourceRoute ~ dateTimeEventRoute)
}

