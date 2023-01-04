/*
 * Copyright 2023 HM Revenue & Customs
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

import models.DatedSessionMap
import models.api.SessionMap
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import utils.Logging

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SessionRepository @Inject()(config: Configuration, mongo: MongoComponent)
  extends PlayMongoRepository[DatedSessionMap](
    mongoComponent = mongo,
    collectionName = config.get[String]("appName"),
    domainFormat = DatedSessionMap.formats,
    indexes = Seq(
      IndexModel(
        ascending("sessionId","transactionId","registrationId"),
        IndexOptions()
          .name("sessionRegistrationIndex")
      ),IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds").toLong, TimeUnit.SECONDS)
      )
    ),
    extraCodecs = Seq(Codecs.playFormatCodec(SessionMap.format)),
    replaceIndexes = config.get[String]("mongodb.replaceIndexes").toBoolean
  ) with Logging {

  collection.listIndexes().map(_.toJson()).toFuture().map { indexes =>
    logger.info(s"Existing Indexes: \n" + indexes.mkString("\n"))
  }
  logger.info(s"Creating SessionRepository with replaceIndexes set to: ${config.get[String]("mongodb.replaceIndexes").toBoolean}")

  def upsertSessionMapByKey(key: String, id: String, sm: SessionMap): Future[Boolean] =
    collection.findOneAndReplace(
      filter = equal(key, id),
      replacement = DatedSessionMap(sm),
      options = FindOneAndReplaceOptions().upsert(true)
    ).toFuture().map(_ => true)


  def upsertSessionMap(sm: SessionMap): Future[Boolean] =
    upsertSessionMapByKey("sessionId", sm.sessionId, sm)



  def removeDocument(id: String): Future[Boolean] = {
    collection.deleteOne(equal("sessionId", id)).toFuture().map(_.wasAcknowledged())
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
