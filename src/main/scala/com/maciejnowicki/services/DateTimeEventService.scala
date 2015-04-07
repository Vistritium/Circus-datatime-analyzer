package com.maciejnowicki.services

import java.text.MessageFormat

import com.maciejnowicki.core.Configs
import com.maciejnowicki.models.DateTimeEvent
import org.joda.time.{Period, DateTime}
import spray.json.{JsString, JsArray}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object DateTimeEventService {

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

  def getData(getDataRequest: GetDataRequest): Either[String, Future[JsArray]] = {
    if (getDataRequest.from.isEmpty && getDataRequest.to.isEmpty) {
      Left("appear and disappear were empty, one must be defined")
    } else {
      val res = {
        val rawEvents = DateTimeEvent.getByDate(getDataRequest.from, getDataRequest.to, getDataRequest.provider)

        getDataRequest.nameFilter match {
          case Some(x) if x.isEmpty || x.forall(_ == ' ') => rawEvents
          case Some(x) => {
            val searches = x.toLowerCase().split(" ")
            rawEvents map {
              events => {
                events.filter(event => {
                  val eventUser = event.user.toLowerCase
                  searches.exists(x => x.contains(eventUser) || eventUser.contains(x))
                })
              }
            }
          }
          case None => rawEvents
        }
      }
      val userAggregatedRes = res map DateTimeEvent.convertToUserAggregatedEvents

      Right(userAggregatedRes)
    }
  }

  def getProviders(): Future[JsArray] = {
    DateTimeEvent.getProviders() map { list =>
      JsArray(list.map(JsString(_)).toVector)
    }
  }

}

case class GetDataRequest(from: Option[DateTime], to: Option[DateTime], nameFilter: Option[String], provider: Option[String])

case class PingEventRequest(provider: String, user: String)