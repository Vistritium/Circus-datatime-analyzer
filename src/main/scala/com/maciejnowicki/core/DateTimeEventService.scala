package com.maciejnowicki.core

import com.maciejnowicki.core.DateTimeEventService.GetDataRequest
import com.maciejnowicki.models.DateTimeEvent
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument
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

  val dateTimeEventRoute =
    path("getData") {
      post {
        entity(as[JsObject]) {
          data => {
            val res = getData(data)

            res match {
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
    }


  def getData(request: JsObject): Either[String, Future[String]] = {

    val getDataRequest = request.convertTo[GetDataRequest]
    if (getDataRequest.from.isEmpty && getDataRequest.to.isEmpty) {
      Left("from and to were empty, one must be defined")
    } else {

      val res = DateTimeEvent.getByDate(getDataRequest.from, getDataRequest.to, getDataRequest.provider)

      val userAggregatedRes = res map DateTimeEvent.convertToUserAggregatedEvents

      val convertedRes = userAggregatedRes.map(x => x.toString)

      Right(convertedRes)
    }
  }

}

object DateTimeEventService {

  case class GetDataRequest(from: Option[DateTime], to: Option[DateTime], nameFilter: Option[String], provider: Option[String]) {

  }


}
