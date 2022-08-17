/*
 * Copyright 2022 HM Revenue & Customs
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

import models.api.SessionMap
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DatedSessionMap(sessionId: String,
                           registrationId: String,
                           transactionId: String,
                           data: Map[String, JsValue],
                           lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object DatedSessionMap {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val formats = Json.format[DatedSessionMap]

  def apply(sessionMap: SessionMap): DatedSessionMap = DatedSessionMap(sessionMap.sessionId, sessionMap.registrationId, sessionMap.transactionId, sessionMap.data)
}

class ReactiveMongoRepository(config: Configuration, mongo: () => DefaultDB)
  extends ReactiveRepository[DatedSessionMap, BSONObjectID](config.get[String]("appName"), mongo, DatedSessionMap.formats) {

  val fieldName = "lastUpdated"
  val sessionRegistrationIndexIndex = "sessionRegistrationIndex"
  val lastUpdatedIndex = "lastUpdatedIndex"

  val expireAfterSeconds = "expireAfterSeconds"
  val timeToLiveInSeconds: Int = config.get[Int]("mongodb.timeToLiveInSeconds")

  createIndex(Seq("sessionId", "transactionId", "registrationId"), sessionRegistrationIndexIndex)
  createTTLIndex(Seq("lastUpdated"), lastUpdatedIndex, timeToLiveInSeconds)

  private def createIndex(fields: Seq[String], indexName: String): Future[Boolean] = {
    collection.indexesManager.ensure(Index(createIndexes(fields), Some(indexName))) map {
      result =>
        result
    } recover {
      case e =>
        logger.error("Failed to set index", e)
        false
    }
  }

  private def createTTLIndex(fields: Seq[String], indexName: String, ttl: Int): Future[Boolean] = {
    collection.indexesManager.ensure(Index(createIndexes(fields), Some(indexName), options = BSONDocument(expireAfterSeconds -> ttl))) map {
      result =>
        logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
    } recover {
      case e =>
        logger.error("Failed to set TTL index", e)
        false
    }
  }

  def createIndexes(fields: Seq[String]):Seq[(String, IndexType)] = fields.map(field => (field, IndexType.Ascending))

  def upsertSessionMap(sm: SessionMap): Future[Boolean] =
    upsertSessionMapByKey("sessionId", sm.sessionId, sm)

  private def upsertSessionMapByKey(key: String, id: String, sm: SessionMap): Future[Boolean] = {
    val selector = Json.obj(key -> id)
    val cmDocument = Json.toJson(DatedSessionMap(sm))
    val modifier = BSONDocument("$set" -> cmDocument)

    collection.update(selector, modifier, upsert = true).map(_.ok)
  }

  def removeDocument(id: String): Future[Boolean] = {
    collection.remove(BSONDocument("sessionId" -> id)).map(_.n > 0)
  }

  def getSessionMap(id: String): Future[Option[SessionMap]] =
    getSessionMapByKey("sessionId", id)


  def getLatestSessionMapByTransactionId(id: String): Future[Option[SessionMap]] =
    getLatestSessionMapByKey("transactionId", id)

  private def getSessionMapByKey(key: String, id: String): Future[Option[SessionMap]] =
    collection.find(Json.obj(key -> id)).one[SessionMap]

  private def getLatestSessionMapByKey(key: String, id: String): Future[Option[SessionMap]] =
    collection.find(Json.obj(key -> id)).sort(Json.obj("lastUpdated" -> -1)).one[SessionMap]
}

@Singleton
class SessionRepository @Inject()(config: Configuration,
                                  reactiveMongoComponent: ReactiveMongoComponent) {

  private lazy val sessionRepository = new ReactiveMongoRepository(config, reactiveMongoComponent.mongoConnector.db)

  def apply(): ReactiveMongoRepository = sessionRepository
}
