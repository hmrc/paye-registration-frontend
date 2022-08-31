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

import java.util.UUID

import connectors.KeystoreConnector
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.api.SessionMap
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json, OWrites}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.test.MongoSupport

class SessionRepositoryISpec extends IntegrationSpecBase with MongoSupport {
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

  class Setup {
    val repository = new ReactiveMongoRepository(app.configuration, mongoComponent)
    val connector = app.injector.instanceOf[KeystoreConnector]
    await(repository.collection.drop().head())
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
  }
}
