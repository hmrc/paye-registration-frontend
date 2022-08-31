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

import java.util.concurrent.TimeUnit

import javax.inject.{Inject, Singleton}
import models.api.SessionMap
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, FindOneAndReplaceOptions, IndexModel, IndexOptions}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DatedSessionMap(sessionId: String,
                           registrationId: String,
                           transactionId: String,
                           data: Map[String, JsValue],
                           lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object DatedSessionMap {
  implicit val dateFormat = MongoJodaFormats.dateTimeFormat
  implicit val formats = Json.format[DatedSessionMap]

  def apply(sessionMap: SessionMap): DatedSessionMap = DatedSessionMap(sessionMap.sessionId, sessionMap.registrationId, sessionMap.transactionId, sessionMap.data)
}

class ReactiveMongoRepository(config: Configuration, mongo: MongoComponent)
  extends PlayMongoRepository[DatedSessionMap](
    mongoComponent = mongo,
    collectionName = config.get[String]("appName"),
    domainFormat = DatedSessionMap.formats,
    indexes = Seq(IndexModel(
        ascending("sessionId","transactionId","registrationId"),
        IndexOptions()
          .name("sessionRegistrationIndex")
      ),IndexModel(
      ascending("lastUpdated"),
      IndexOptions()
        .name("lastUpdatedIndex")
        .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds").toLong, TimeUnit.SECONDS)
    )),
    extraCodecs = Seq(Codecs.playFormatCodec(SessionMap.format))
  )  {

  def upsertSessionMapByKey(key: String, id: String, sm: SessionMap): Future[Boolean] =
    collection.findOneAndReplace(
      filter = equal(key, id),
      replacement = DatedSessionMap(sm),
      options = FindOneAndReplaceOptions().upsert(true)
    ).toFuture().map(_ => true)


  def upsertSessionMap(sm: SessionMap): Future[Boolean] =
    upsertSessionMapByKey("sessionId", sm.sessionId, sm)



  def removeDocument(id: String): Future[Boolean] = {
    collection.deleteOne(equal("sessionId", id)).map(_.wasAcknowledged()).head()
  }

  def getSessionMap(id: String): Future[Option[SessionMap]] =
    getSessionMapByKey("sessionId",id)


  def getLatestSessionMapByTransactionId(id: String): Future[Option[SessionMap]] =
    getLatestSessionMapByKey("transactionId", id)

  private def getSessionMapByKey(key: String,id: String): Future[Option[SessionMap]] =
    collection.find[SessionMap](equal(key, id)).headOption()

  private def getLatestSessionMapByKey(key: String, id: String): Future[Option[SessionMap]] =
    collection.find[SessionMap](equal(key, id)).sort(equal("lastUpdated", -1)).headOption()
}

@Singleton
class SessionRepository @Inject()(config: Configuration, reactiveMongoComponent: MongoComponent) {

  private lazy val sessionRepository: ReactiveMongoRepository = new ReactiveMongoRepository(config, reactiveMongoComponent)

  def apply(): ReactiveMongoRepository = sessionRepository
}
