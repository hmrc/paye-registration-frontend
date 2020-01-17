/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json

import scala.concurrent.Future

class KeystoreConnectorSpec extends PayeComponentSpec {

  val connector = new KeystoreConnector {
    override val metricsService       = new MockMetrics
    override val sessionCache         = mockSessionCache
    override val sessionRepository    = mockSessionRepository
    override val successCounter       = metricsService.keystoreSuccessResponseCounter
    override val failedCounter        = metricsService.keystoreFailedResponseCounter
    override val emptyResponseCounter = metricsService.keystoreFailedResponseCounter
    override def timer   = metricsService.keystoreResponseTimer.time()
  }

  case class TestModel(test: String)
  object TestModel {
    implicit val formats = Json.format[TestModel]
  }

  "Saving into SessionRepository" should {
    "save the model" in {
      val testModel = TestModel("test")

      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(testModel)))

      when(mockSessionRepository()).thenReturn(mockReactiveMongoRepo)

      when(mockReactiveMongoRepo.upsertSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val result = await(connector.cache[TestModel]("testKey", "testRegId", "testTxId", testModel))
      result mustBe returnSessionMap
    }
  }

  "Fetching and getting from SessionRepository" should {
    "return a list" in {
      val testModel = TestModel("test")
      val list = List(testModel)
      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(list)))

      when(mockSessionCache.fetchAndGetEntry[List[TestModel]](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(list)))

      when(mockSessionRepository()).thenReturn(mockReactiveMongoRepo)

      when(mockReactiveMongoRepo.getSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(returnSessionMap)))

      val result = await(connector.fetchAndGet[List[TestModel]]("testKey"))
      result mustBe Some(list)
    }
  }

  "Fetching and getting from Keystore & saving into SessionRepository" should {
    "return a CurrentProfile" in {
      val cp = CurrentProfile("regId", CompanyRegistrationProfile("held", "txId", None), "", false, None)
      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(cp)))

      when(mockSessionCache.fetchAndGetEntry[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(cp)))

      when(mockSessionRepository()).thenReturn(mockReactiveMongoRepo)

      when(mockReactiveMongoRepo.upsertSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val result = await(connector.fetchAndGetFromKeystore("testKey"))
      result mustBe Some(cp)
    }
  }

  "Fetching from SessionRepository" should {
    "return a CacheMap" in {
      val testModel = TestModel("test")

      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(testModel)))

      when(mockSessionRepository()).thenReturn(mockReactiveMongoRepo)

      when(mockReactiveMongoRepo.getSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(returnSessionMap)))

      val result = await(connector.fetch())
      result mustBe Some(returnSessionMap)
    }
  }

  "Removing from SessionRepository" should {
    "return a HTTP Response" in {

      val testModel = TestModel("test")
      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(testModel)))

      when(mockSessionRepository()).thenReturn(mockReactiveMongoRepo)

      when(mockReactiveMongoRepo.removeDocument(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val result = await(connector.remove())
      result mustBe true
    }
  }
}
