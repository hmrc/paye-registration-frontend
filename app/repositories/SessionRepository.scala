/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import common.Logging
import common.exceptions.InternalExceptions.RegistrationIdMismatchException
import enums.IncorporationStatus
import javax.inject.{Inject, Singleton}
import models.api.SessionMap
import models.external.CurrentProfile
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DatedSessionMap(sessionId: String,
                           registrationId: String,
                           transactionId: String,
                           data: Map[String, JsValue],
                           lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object DatedSessionMap {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val formats    = Json.format[DatedSessionMap]

  def apply(sessionMap: SessionMap): DatedSessionMap = DatedSessionMap(sessionMap.sessionId, sessionMap.registrationId, sessionMap.transactionId, sessionMap.data)
}

class ReactiveMongoRepository(config: Configuration, mongo: () => DefaultDB)
  extends ReactiveRepository[DatedSessionMap, BSONObjectID](config.getString("appName").get, mongo, DatedSessionMap.formats) {

  val fieldName        = "lastUpdated"
  val createdIndexName = "currentProfileIndex"

  val expireAfterSeconds = "expireAfterSeconds"
  val timeToLiveInSeconds: Int = config.getInt("mongodb.timeToLiveInSeconds").get

  createIndex(Seq("sessionId", "transactionId", "registrationId"), createdIndexName, timeToLiveInSeconds)

  private def createIndex(fields: Seq[String], indexName: String, ttl: Int): Future[Boolean] = {
    val indexes = fields.map(field => (field, IndexType.Ascending))
    collection.indexesManager.ensure(Index(indexes, Some(indexName), options = BSONDocument(expireAfterSeconds -> ttl))) map {
      result =>
        logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
    } recover {
      case e =>
        logger.error("Failed to set TTL index", e)
        false
    }
  }

  def upsertSessionMap(sm: SessionMap): Future[Boolean] = {
    val selector   = Json.obj("sessionId" -> sm.sessionId)
    val cmDocument = Json.toJson(DatedSessionMap(sm))
    val modifier   = BSONDocument("$set" -> cmDocument)

    collection.update(selector, modifier, upsert = true).map(_.ok)
  }

  def removeDocument(id: String): Future[Boolean] = {
    collection.remove(BSONDocument("sessionId" -> id)).map(_.ok)
  }

  def getSessionMap(id: String): Future[Option[SessionMap]] = {
    collection.find(Json.obj("sessionId" -> id)).one[SessionMap]
  }

  def setIncorpStatus(id: String, status: IncorporationStatus.Value): Future[Boolean] = {
    val selector = BSONDocument("transactionId" -> id)
    val modifier = BSONDocument("$set" -> BSONDocument("data.CurrentProfile.incorpStatus" -> Json.toJson(status)))

    collection.update(selector, modifier) map(_.nModified == 1)
  }

  def getRegistrationID(transactionID: String): Future[Option[String]] = {
    collection.find(Json.obj("transactionId" -> transactionID)).one[SessionMap].map {
      _.fold(Option.empty[String]) { sessionMap =>
        sessionMap.getEntry[CurrentProfile]("CurrentProfile").collect {
          case cp if cp.registrationID == sessionMap.registrationId => cp.registrationID
          case cp => throw new RegistrationIdMismatchException(sessionMap.registrationId, cp.registrationID)
        }
      }

      //_.flatMap(_.getEntry[CurrentProfile]("CurrentProfile").map(_.registrationID))
    }
  }

}

@Singleton
class SessionRepository @Inject()(config: Configuration) {

  class DbConnection extends MongoDbConnection

  private lazy val sessionRepository = new ReactiveMongoRepository(config, new DbConnection().db)

  def apply(): ReactiveMongoRepository = sessionRepository
}
