package com.maciejnowicki.models

import com.maciejnowicki.core.MongoDB
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, _}
import reactivemongo.core.commands.Count

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

case class DataTimeEvent(id: String, provider: String, user: String, from: DateTime, to: DateTime)

object DataTimeEvent {

  val collectionName = "dataTimeEvents";
  val collection = MongoDB.db[BSONCollection]("dataTimeEvents")


  def get(id: String): Future[Option[DataTimeEvent]] = {
    val findByIdQuery = BSONDocument("id" -> id)
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

  /**
   * @return id if inserted new element
   */
  def insertUpdate(dataTimeEvent: DataTimeEvent): Future[Option[String]] = {
    val query = BSONDocument("id" -> dataTimeEvent.id)
    val count = Count(
      collection.name,
      query = Some(query)
    )
    val existsFuture = MongoDB.db.command(count).map(_ > 0)

    existsFuture.map {
      exists => {
        if (exists) {
          val resultIdFuture = insert(dataTimeEvent)
          Some(resultIdFuture)
        } else {
          update(dataTimeEvent)
          None
        }
      }
    }
  }

  private[this] def update(dataTimeEvent: DataTimeEvent): Unit = {
    val doc = DataTimeEventFormat.write(dataTimeEvent)
    val modifier = BSONDocument(
      "$set" -> doc
    )
    val futureUpdate = collection.update(BSONDocument("_id" -> dataTimeEvent.id), modifier)
    futureUpdate.onComplete {
      case Failure(e) => throw e
    }
  }


  private[this] def insert(dataTimeEvent: DataTimeEvent): String = {
    val id = BSONObjectID.generate.stringify
    val dataWithGeneratedId = dataTimeEvent.copy(id = id)
    collection.insert(DataTimeEventFormat.write(dataWithGeneratedId))
    id
  }

}

object DataTimeEventFormat extends BSONDocumentReader[DataTimeEvent] with BSONDocumentWriter[DataTimeEvent] {
  val fmt = ISODateTimeFormat.dateTime();

  override def write(event: DataTimeEvent): BSONDocument = BSONDocument(
    "_id" -> event.id,
    "provider" -> event.provider,
    "user" -> event.user,
    "from" -> fmt.print(event.from),
    "to" -> fmt.print(event.to)
  )

  override def read(bson: BSONDocument): DataTimeEvent = {
    DataTimeEvent(
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