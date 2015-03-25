package com.maciejnowicki.models

import com.maciejnowicki.core.MongoDB
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, _}
import reactivemongo.core.commands.Count

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class DateTimeEvent(id: String, provider: String, user: String, from: DateTime, to: DateTime)

object DateTimeEvent {

  val collectionName = "dateTimeEvents";
  val collection = MongoDB.db[BSONCollection]("dateTimeEvents")


  def deleteAllByProvider(provider: String): Future[Int] = {
    val query = BSONDocument("provider" -> provider)
    val remove = collection.remove(query)
    remove.map(_.n)
  }

  def getLatestByFromDate(provider: String, user: String): Future[Option[DateTimeEvent]] = {

    val query = BSONDocument("provider" -> provider, "user" -> user)
    val sort = BSONDocument("fromDate" -> -1)

    val result = collection.find(query).sort(sort).one


    result.map(_.map {
      doc => {
        DataTimeEventFormat.read(doc)
      }
    })
  }


  def get(id: String): Future[Option[DateTimeEvent]] = {
    val findByIdQuery = BSONDocument("_id" -> id)
    collection.find(findByIdQuery).one.map {
      docFuture => {
        docFuture.map {
          doc => {
            DataTimeEventFormat.read(doc)
          }
        }
      }
    }
  }

  def getAnyElementByProvider(provider: String): Future[Option[DateTimeEvent]] = {
    val query = BSONDocument("provider" -> provider)
    val futureOptionDoc = collection.find(query).one
    futureOptionDoc map {
      _ map {
        doc => DataTimeEventFormat.read(doc)
      }
    }
  }

  /**
   * @return id if inserted new element
   */
  def insertUpdate(dataTimeEvent: DateTimeEvent): Future[Option[String]] = {
    val query = BSONDocument("_id" -> dataTimeEvent.id)
    val count = Count(
      collection.name,
      query = Some(query)
    )
    val elements = MongoDB.db.command(count)
    val existsFuture = elements.map(_ > 0)

    val result = existsFuture.map {
      exists => {
        val futureOptStr = if (exists) {
          update(dataTimeEvent)
          Future {
            None
          }.mapTo[Option[String]]
        } else {
          val resultIdFuture = insert(dataTimeEvent).map(Some(_))
          resultIdFuture: Future[Option[String]]
        }
        futureOptStr
      }
    }
    result.flatMap(identity)
  }

  private[this] def update(dataTimeEvent: DateTimeEvent): Unit = {
    val doc = DataTimeEventFormat.write(dataTimeEvent)
    val modifier = BSONDocument(
      "$set" -> doc
    )
    val futureUpdate = collection.update(BSONDocument("_id" -> dataTimeEvent.id), modifier)
    futureUpdate.onComplete {
      case Failure(e) => throw e
      case Success(lastError) =>
    }
  }


  private[this] def insert(dataTimeEvent: DateTimeEvent): Future[String] = {
    val id = BSONObjectID.generate.stringify
    val dataWithGeneratedId = dataTimeEvent.copy(id = id)
    collection.insert(DataTimeEventFormat.write(dataWithGeneratedId)).map(x => id)
  }

}

object DataTimeEventFormat extends BSONDocumentReader[DateTimeEvent] with BSONDocumentWriter[DateTimeEvent] {
  val fmt = ISODateTimeFormat.dateTime();

  override def write(event: DateTimeEvent): BSONDocument = BSONDocument(
    "_id" -> event.id,
    "provider" -> event.provider,
    "user" -> event.user,
    "from" -> fmt.print(event.from),
    "to" -> fmt.print(event.to)
  )

  override def read(bson: BSONDocument): DateTimeEvent = {
    DateTimeEvent(
      bson.getAs[String]("_id").get,
      bson.getAs[String]("provider").get,
      bson.getAs[String]("user").get,
      toDataTime(bson.getAs[String]("from").get),
      toDataTime(bson.getAs[String]("to").get)
    )
  }

  def toDataTime(isoDataTime: String): DateTime = {
    DateTime.parse(isoDataTime, fmt)
  }


}