package com.maciejnowicki.core

import com.maciejnowicki.utils.TimeUtils
import org.joda.time.DateTime
import spray.json._

object AppJsonProtocol extends DefaultJsonProtocol{

  implicit object YodaDateTimeConverter extends RootJsonFormat[DateTime] {

    override def write(obj: DateTime): JsValue = JsString(obj.toString(TimeUtils.dateTimeFormatter))

    override def read(json: JsValue): DateTime = json match {
      case JsString(x) => TimeUtils.dateTimeFormatter.parseDateTime(x)
      case _ => throw new DeserializationException("JsString expected")
    }

  }

}
