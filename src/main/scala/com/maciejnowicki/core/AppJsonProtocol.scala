package com.maciejnowicki.core

import com.maciejnowicki.models.DateTimeEvent
import com.maciejnowicki.utils.TimeUtils
import org.joda.time.DateTime
import spray.json._

object AppJsonProtocol extends DefaultJsonProtocol {

  implicit object YodaDateTimeConverter extends RootJsonFormat[DateTime] {

    override def write(obj: DateTime): JsValue = JsString(obj.toString(TimeUtils.dateTimeFormatter))

    override def read(json: JsValue): DateTime = json match {
      case JsString(x) => TimeUtils.dateTimeFormatter.parseDateTime(x)
      case _ => throw new DeserializationException("JsString expected")
    }

  }

  implicit object DateEventConverter extends RootJsonFormat[DateTimeEvent] {
    override def write(obj: DateTimeEvent): JsValue = JsObject(
      obj.user -> JsString("user"),
      obj.provider -> JsString("provider"),
      "appear" -> YodaDateTimeConverter.write(obj.from),
      "disappear" -> YodaDateTimeConverter.write(obj.to),
      "id" -> JsString(obj.id)
    )

    override def read(json: JsValue): DateTimeEvent = json match {
      case JsObject(x) => {
        DateTimeEvent(
          x.getOrElse("id", JsString("-1")).asInstanceOf[JsString].value,
          x("provider").asInstanceOf[JsString].value,
          x("user").asInstanceOf[JsString].value,
          YodaDateTimeConverter.read(x("appear")),
          YodaDateTimeConverter.read(x("disappear"))
        )
      }
    }
  }

}
