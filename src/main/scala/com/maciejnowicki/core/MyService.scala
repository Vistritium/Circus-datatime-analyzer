package com.maciejnowicki.core

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.http.StatusCodes
import spray.routing._
import StatusCodes._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  implicit val myRejectionHandler = RejectionHandler {
    case MissingCookieRejection(cookieName) :: _ =>
      complete(BadRequest, "No cookies, no service!!!")
    case MethodRejection(supported) :: _ => {
      complete(NotFound, "ojojoj " + supported.toString())
    }
  }

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}



// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/plain`) {
          // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            "kappa"
          }
        }
      }
    } ~ path("maciej" ) {
      get {
        respondWithMediaType(`text/plain`) {
          complete {
            "Siemanko "
          }
        }
      }
    }
}
