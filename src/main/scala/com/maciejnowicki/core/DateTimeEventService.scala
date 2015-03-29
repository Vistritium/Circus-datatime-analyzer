package com.maciejnowicki.core

import java.text.MessageFormat

import com.maciejnowicki.core.DateTimeEventService.{PingEventRequest, GetDataRequest}
import com.maciejnowicki.models.DateTimeEvent
import org.joda.time.{Period, DateTime}
import reactivemongo.bson.{BSONArray, BSONValue, BSONDocument}
import spray.http.MediaType
import spray.json
import spray.json.JsObject
import spray.routing.HttpService
import spray.json._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import AppJsonProtocol._
import spray.http.StatusCodes._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.MediaTypes._


import scala.concurrent.Future
import scala.util.parsing.json.JSONObject

trait DateTimeEventService extends HttpService {
  implicit val getDataRequestFormat = jsonFormat4(GetDataRequest)
  implicit val eventDataFormat = jsonFormat5(DateTimeEvent.apply)
  implicit val pingEventFormat = jsonFormat2(PingEventRequest)

  val dateTimeEventRoute =
    path("getData") {
      post {
        entity(as[JsObject]) {
          data => {
            getData(data) match {
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
            complete(pingEvent(pingEventReq))
          }
        }
      }
    } ~ path("pingEvents") {
      post {
        entity(as[List[PingEventRequest]]){
          pingEventRequests => {
            val futuresRes = pingEventRequests.map(pingEvent(_))
            val futureRes: Future[List[String]] = Future.sequence(futuresRes)
            val futureStringRes = futureRes.map(_.mkString("\n"))

            complete(futureStringRes)
          }
        }
      }
    } ~ path("providers") {
      get {
        complete(getProviders())
      }
    }

  def getProviders(): Future[JsArray] ={
    DateTimeEvent.getProviders() map { list =>
      JsArray(list.map(JsString(_)).toVector)
    }
  }

  def updateEvent() = {

  }

  def pingEvent(pingEventRequest: PingEventRequest): Future[String] = {

    def insertNew() = {
      val dateTimeToInsert = DateTimeEvent("-1", pingEventRequest.provider, pingEventRequest.user, DateTime.now(), DateTime.now().plusSeconds(1))
      DateTimeEvent.insertUpdate(dateTimeToInsert)
      "inserted new element"
    }

    DateTimeEvent.getLatestByFromDate(pingEventRequest.provider, pingEventRequest.user) map {
      case Some(latest) => {
        val now = DateTime.now()
        val last = latest.to

        val periodInSecs = new Period(last, now).toStandardSeconds.getSeconds

        periodInSecs match {
          case x if x <= Configs.eventPingTolerance => {
            val updated = latest.copy(to = DateTime.now())
            DateTimeEvent.insertUpdate(updated)
            MessageFormat.format("updated current element of id {0} because interval in sec was {1}", updated.id, periodInSecs.toString)
          }
          case _ =>
            insertNew()
        }

      }
      case None => insertNew()
    }
  }


  def getData(request: JsObject): Either[String, Future[JsArray]] = {

    val getDataRequest = request.convertTo[GetDataRequest]
    if (getDataRequest.from.isEmpty && getDataRequest.to.isEmpty) {
      Left("from and to were empty, one must be defined")
    } else {

      val res = DateTimeEvent.getByDate(getDataRequest.from, getDataRequest.to, getDataRequest.provider)

      val userAggregatedRes = res map DateTimeEvent.convertToUserAggregatedEvents

      Right(userAggregatedRes)
    }
  }

}

object DateTimeEventService {

  case class GetDataRequest(from: Option[DateTime], to: Option[DateTime], nameFilter: Option[String], provider: Option[String]) {

  }

  case class PingEventRequest(provider: String, user: String)


}
