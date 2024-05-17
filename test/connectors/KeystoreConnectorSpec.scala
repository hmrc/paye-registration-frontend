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

package connectors

import com.codahale.metrics.{Counter, Timer}
import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.MetricsService

import scala.concurrent.{ExecutionContext, Future}

class KeystoreConnectorSpec extends PayeComponentSpec {

  val mockMetricsService: MetricsService = app.injector.instanceOf[MetricsService]

  val connector: KeystoreConnector = new KeystoreConnector (
    mockSessionCache,
    mockMetricsService,
    mockSessionRepository
  ) {
    override val successCounter: Counter = metricsService.keystoreSuccessResponseCounter
    override val failedCounter: Counter = metricsService.keystoreFailedResponseCounter
    override val emptyResponseCounter: Counter = metricsService.keystoreFailedResponseCounter

    override def timer: Timer.Context = metricsService.keystoreResponseTimer.time()
    override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  implicit val request: FakeRequest[_] = FakeRequest()

  case class TestModel(test: String)

  object TestModel {
    implicit val formats = Json.format[TestModel]
  }

  "Saving into SessionRepository" should {
    "save the model" in {
      val testModel = TestModel("test")

      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(testModel)))

      when(mockSessionRepository.upsertSessionMap(ArgumentMatchers.any()))
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

      when(mockSessionRepository.getSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(returnSessionMap)))

      val result = await(connector.fetchAndGet[List[TestModel]]("testKey"))
      result mustBe Some(list)
    }
  }

  "Fetching and getting from Keystore & saving into SessionRepository" should {
    "return a CurrentProfile" in {
      val cp = CurrentProfile("regId", CompanyRegistrationProfile("held", "txId", None), "", false, None)

      when(mockSessionCache.fetchAndGetEntry[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(cp)))

      when(mockSessionRepository.upsertSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val result = await(connector.fetchAndGetFromKeystore("testKey"))
      result mustBe Some(cp)
    }
  }

  "Fetching from SessionRepository" should {
    "return a CacheMap" in {
      val testModel = TestModel("test")

      val returnSessionMap = SessionMap("testSessionId", "testRegId", "testTxId", Map("testKey" -> Json.toJson(testModel)))

      when(mockSessionRepository.getSessionMap(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(returnSessionMap)))

      val result = await(connector.fetch())
      result mustBe Some(returnSessionMap)
    }
  }

  "Removing from SessionRepository" should {
    "return a HTTP Response" in {

      when(mockSessionRepository.removeDocument(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val result = await(connector.remove())
      result mustBe true
    }
  }
}
