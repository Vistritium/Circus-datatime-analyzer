package com.maciejnowicki.events.models

import java.text.MessageFormat
import java.util.concurrent.locks.ReentrantLock
import com.maciejnowicki.events.core.MongoDB
import com.maciejnowicki.events.utils.TimeUtils
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, _}
import reactivemongo.core.commands.Count
import spray.json.{JsArray, JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


case class DateTimeEvent(id: String, provider: String, user: String, from: DateTime, to: DateTime, description: Option[String] = None)

object DateTimeEvent extends StrictLogging{

  val updateProviderLock = new ReentrantLock()

  private val collectionName = "dateTimeEvents";
  private val collection = MongoDB.db[BSONCollection](collectionName)

  private val providersCollection = MongoDB.db[BSONCollection]("dateTimeProviders")

  private def updateProviders(provider: String): Unit = {



    val future = getProviders() map {
      currentProviders => {
        if(!currentProviders.map(_.toLowerCase).contains(provider.toLowerCase)){

          updateProviderLock.lock()
          try {
            val currentProviders = Await.result(getProviders(), 30 seconds)
            if(!currentProviders.map(_.toLowerCase).contains(provider.toLowerCase)){
              logger.info("New provider: " + provider)
              Await.result(insertProvider(provider), 50 seconds)
            }
          } finally {
            updateProviderLock.unlock()
          }
        }
      }
    }

    Await.result(future, 1 minute)
  }

  private def insertProvider(provider: String) = {
    providersCollection.insert(BSONDocument("provider" -> provider))
  }

  def getProviders(): Future[List[String]] = {

    val providersQuery = providersCollection.find(BSONDocument())

    val providers = providersQuery.cursor.collect[List]()

    providers map {
      _ map {
        doc => {
          doc.getAs[String]("provider").get
        }
      }
    }


  }


  def getByDate(from: Option[DateTime], to: Option[DateTime] = None, provider: Option[String] = None, user: Option[String] = None): Future[List[DateTimeEvent]] = {

    require(from.isDefined || to.isDefined, "from or to date must be defined")

    logger.info(s"geting from $from to $to with provider $provider with user $user")

    val fromQuery = from.map(from => {
      BSONDocument("to" -> BSONDocument("$gte" -> BSONDateTime(from.getMillis)))
    })

    val toQuery = to.map(to => {
      BSONDocument("from" -> BSONDocument("$lt" -> BSONDateTime(to.getMillis)))
    })

    val providerQuery = provider.map(provider => {
      BSONDocument("provider" -> provider)
    })

    val userQuery = user.map(user => {
      BSONDocument("user" -> user)
    })

    val query = (fromQuery :: toQuery :: providerQuery :: userQuery :: Nil).flatten.reduce((x, y) => x.add(y))

    collection.find(query).cursor.collect[List]().map {
      result => {
        val size = result.size
        logger.info(MessageFormat.format("returning {0} elements", size.toString))
        result.map(DateTimeEventFormat.read(_))
      }
    }

  }


  def deleteAllByProvider(provider: String): Future[Int] = {
    val query = BSONDocument("provider" -> provider)
    val remove = collection.remove(query)
    remove.map(_.n)
  }

  def getLatestByFromDate(provider: String, user: String): Future[Option[DateTimeEvent]] = {

    val query = BSONDocument("provider" -> provider, "user" -> user)
    val sort = BSONDocument("from" -> -1)

    val result = collection.find(query).sort(sort).one


    result.map(_.map {
      doc => {
        DateTimeEventFormat.read(doc)
      }
    })
  }


  def get(id: String): Future[Option[DateTimeEvent]] = {
    val findByIdQuery = BSONDocument("_id" -> id)
    collection.find(findByIdQuery).one.map {
      docFuture => {
        docFuture.map {
          doc => {
            DateTimeEventFormat.read(doc)
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
        doc => DateTimeEventFormat.read(doc)
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
    val doc = DateTimeEventFormat.write(dataTimeEvent)
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
    updateProviders(dataTimeEvent.provider)
    val id = BSONObjectID.generate.stringify
    val dataWithGeneratedId = dataTimeEvent.copy(id = id)
    collection.insert(DateTimeEventFormat.write(dataWithGeneratedId)).map(x => id)
  }


  def convertToUserAggregatedEvents(events: List[DateTimeEvent]): JsArray = {
    val userGrouped = events.groupBy(_.user)

    val result = userGrouped.map(x => {

      val list = x._2.map(event => {
        JsObject(
          "appear" -> JsString(event.from.toString(TimeUtils.dateTimeFormatter)),
          "disappear" -> JsString(event.to.toString(TimeUtils.dateTimeFormatter))
        )
      })
      val jsArray: JsArray = JsArray(list.toVector)

      JsObject(
        "name" -> JsString(x._1),
        "times" -> jsArray)

    })

    JsArray(result.toVector)
  }

}

object DateTimeEventFormat extends BSONDocumentReader[DateTimeEvent] with BSONDocumentWriter[DateTimeEvent] {
  val fmt = ISODateTimeFormat.dateTime();

  override def write(event: DateTimeEvent): BSONDocument = BSONDocument(
    "_id" -> event.id,
    "provider" -> event.provider,
    "user" -> event.user,
    "from" -> BSONDateTime(event.from.getMillis),
    "to" -> BSONDateTime(event.to.getMillis),
    "description" -> event.description
  )

  override def read(bson: BSONDocument): DateTimeEvent = {
    DateTimeEvent(
      bson.getAs[String]("_id").get,
      bson.getAs[String]("provider").get,
      bson.getAs[String]("user").get,
      toDataTime(bson.getAs[BSONDateTime]("from").get),
      toDataTime(bson.getAs[BSONDateTime]("to").get),
      bson.getAs[String]("description")
    )
  }

  def toDataTime(bsonDataTime: BSONDateTime): DateTime = {
    new DateTime(bsonDataTime.value)
  }


}