package com.maciejnowicki.core

import com.maciejnowicki.core.AppJsonProtocol._
import com.maciejnowicki.models.DateTimeEvent
import com.maciejnowicki.services.{DateTimeEventService, GetDataRequest, PingEventRequest}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport.{sprayJsonMarshaller, sprayJsonUnmarshaller}
import spray.json.JsObject
import spray.routing.HttpService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DateTimeEventController extends HttpService {
  implicit val getDataRequestFormat = jsonFormat4(GetDataRequest)
  implicit val eventDataFormat = jsonFormat5(DateTimeEvent.apply)
  implicit val pingEventFormat = jsonFormat2(PingEventRequest)

  val dateTimeEventRoute =
    path("getData") {
      post {
        entity(as[JsObject]) {
          data => {
            val getDataRequest = data.convertTo[GetDataRequest]
            DateTimeEventService.getData(getDataRequest) match {
              case Left(a) => {
                complete(BadRequest, a)
              }
              case Right(b) => {
                respondWithMediaType(`application/json`)
                complete(b)
              }
            }
          }
        }
      }
    } ~ path("updateEvent") {
      post {
        entity(as[DateTimeEvent]) {
          dateTimeEvent =>
            complete(dateTimeEvent)
        }
      }
    } ~ path("pingEvent") {
      post {
        entity(as[PingEventRequest]) {
          pingEventReq => {
            complete(DateTimeEventService.pingEvent(pingEventReq))
          }
        }
      }
    } ~ path("pingEvents") {
      post {
        entity(as[List[PingEventRequest]]) {
          pingEventRequests => {
            val futuresRes = pingEventRequests.map(DateTimeEventService.pingEvent(_))
            val futureRes: Future[List[String]] = Future.sequence(futuresRes)
            val futureStringRes = futureRes.map(_.mkString("\n"))

            complete(futureStringRes)
          }
        }
      }
    } ~ path("providers") {
      get {
        complete(DateTimeEventService.getProviders())
      }
    }



  def updateEvent() = {

  }






}

object DateTimeEventController {




}
