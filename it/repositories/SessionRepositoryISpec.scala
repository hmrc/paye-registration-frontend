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

import connectors.KeystoreConnector
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.api.SessionMap
import org.mockito.Mockito.when
import org.mongodb.scala.MongoCommandException
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json, OWrites}
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.test.MongoSupport

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class Index(name: String, expireAfterSeconds: Option[Int])
object Index {
  implicit val format = Json.format[Index]
}

class SessionRepositoryISpec extends IntegrationSpecBase with MongoSupport with MockitoSugar {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val config = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "microservice.services.incorporation-information.uri" -> "/test-incorporation-information",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "mongodb.uri" -> s"$mongoUri"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  val sId = UUID.randomUUID().toString
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

  class Setup(replaceIndexes: Boolean = false) {

    val mockConfiguration = mock[Configuration]
    val expireAfter = app.configuration.get[Int]("mongodb.timeToLiveInSeconds")

    when(mockConfiguration.get[String]("mongodb.replaceIndexes")).thenReturn(replaceIndexes.toString)
    when(mockConfiguration.get[String]("appName")).thenReturn(app.configuration.get[String]("appName"))
    when(mockConfiguration.get[Int]("mongodb.timeToLiveInSeconds")).thenReturn(expireAfter)

    val repository = new SessionRepository(mockConfiguration, mongoComponent)
    val connector = app.injector.instanceOf[KeystoreConnector]
    await(repository.collection.drop().toFuture())
    await(repository.ensureIndexes)

    implicit val jsObjWts: OWrites[JsObject] = OWrites(identity)

    def count = await(repository.collection.countDocuments().head())
  }

  "SessionRepository" should {
    val sessionMap = SessionMap(sId, "regId", "txId", Map("test" -> Json.obj()))


    "cache" when {
      "given a new session map" in new Setup() {
        await(repository.upsertSessionMap(sessionMap))
        count mustBe 1
      }
      "given an updated session map" in new Setup() {
        await(repository.upsertSessionMap(sessionMap))
        count mustBe 1

        val newSessionMap: SessionMap = sessionMap.copy(data = Map("newKey" -> Json.obj("prop1" -> "val1")))
        await(repository.upsertSessionMap(newSessionMap))
        count mustBe 1
        await(repository.getSessionMap(sId)) mustBe Some(newSessionMap)
      }
    }
    "fetch" when {
      "given a session map exists" in new Setup() {
        await(repository.upsertSessionMap(sessionMap))
        count mustBe 1

        await(repository.getSessionMap(sId)) mustBe Some(sessionMap)
      }
    }
    "remove" when {
      "there is a session map to remove" in new Setup() {
        await(repository.upsertSessionMap(sessionMap))
        count mustBe 1

        await(repository.removeDocument(sId)) mustBe true
        count mustBe 0
      }
      "there is no session map to remove" in new Setup() {
        await(repository.removeDocument("wrongSessionId"))
        count mustBe 0
      }
    }

    "getLatestSessionMapByTransactionId" when {
      "there are many session map with the same transaction id" in new Setup() {
        await(repository.upsertSessionMap(sessionMap))
        val sessionMap2 = sessionMap.copy(sessionId = "123456")
        await(repository.upsertSessionMap(sessionMap2))
        await(repository.upsertSessionMap(sessionMap.copy(sessionId = "098765", transactionId = "otherTxId")))
        count mustBe 3

        await(repository.getLatestSessionMapByTransactionId(sessionMap.transactionId)) mustBe Some(sessionMap2)
      }
    }

    "Ensuring Indexes" when {

      def waitForMongoBackgroundTask[T](f: => Future[T], retryAttempt: Int = 0, nTimesToRetry: Int = 10): T =
        await(f recover {
          case e: MongoCommandException if e.getCode == 12587 =>
            if(retryAttempt < nTimesToRetry) {
              Thread.sleep(1000)
              waitForMongoBackgroundTask(f, retryAttempt + 1, nTimesToRetry)
            } else {
              throw e
            }
        })

      def listIndexes(repository: SessionRepository): Seq[Index] =
        waitForMongoBackgroundTask(repository.collection.listIndexes().map(indexDoc => Json.parse(indexDoc.toJson).as[Index]).toFuture()).sortBy(_.name)

      "when `replaceIndexes` is `false`" must {

        "not allow indexes to be replaced with different implementations" in new Setup() {

          listIndexes(repository) mustBe Seq(
            Index("_id_", None),
            Index("lastUpdatedIndex", Some(expireAfter)),
            Index("sessionRegistrationIndex", None)
          )

          //Delete the indexes
          waitForMongoBackgroundTask(repository.collection.dropIndexes().toFuture())

          //Create an index with a different expiresAfter
          val newExpireAfter = 12

          waitForMongoBackgroundTask(repository.collection.createIndexes(Seq(
            IndexModel(
              ascending("lastUpdated"), IndexOptions().name("lastUpdatedIndex").expireAfter(newExpireAfter, TimeUnit.SECONDS)
            )
          )).toFuture())

          //Check New Index with New Expire After created
          listIndexes(repository) mustBe Seq(
            Index("_id_", None),
            Index("lastUpdatedIndex", Some(newExpireAfter))
          )

          //Ensure the repo indexes and expect Code:85 error (index exists with different options)
          intercept[MongoCommandException](waitForMongoBackgroundTask(repository.ensureIndexes)).getCode mustBe 85
        }
      }

      "when `replaceIndexes` is `true`" must {

        "allow indexes to be replaced with different implementations" in new Setup(replaceIndexes = true) {

          listIndexes(repository) mustBe Seq(
            Index("_id_", None),
            Index("lastUpdatedIndex", Some(expireAfter)),
            Index("sessionRegistrationIndex", None)
          )

          //Delete the indexes
          waitForMongoBackgroundTask(repository.collection.dropIndexes().toFuture())

          //Create an index with a different expiresAfter
          val newExpireAfter = 12

          waitForMongoBackgroundTask(repository.collection.createIndexes(Seq(
            IndexModel(
              ascending("lastUpdated"), IndexOptions().name("lastUpdatedIndex").expireAfter(newExpireAfter, TimeUnit.SECONDS)
            )
          )).toFuture())

          //Check New Index with New Expire After created
          listIndexes(repository) mustBe Seq(
            Index("_id_", None),
            Index("lastUpdatedIndex", Some(newExpireAfter))
          )

          //Ensure the repo indexes
          waitForMongoBackgroundTask(repository.ensureIndexes)

          //Check the index has been replaced back to the expireAfter setting from ApplicationConf
          listIndexes(repository) mustBe Seq(
            Index("_id_", None),
            Index("lastUpdatedIndex", Some(expireAfter)),
            Index("sessionRegistrationIndex", None)
          )
        }
      }
    }
  }
}
