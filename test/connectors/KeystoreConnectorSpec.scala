/*
 * Copyright 2017 HM Revenue & Customs
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

import mocks.MockMetrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class KeystoreConnectorSpec extends PAYERegSpec {

  val connector = new KeystoreConnect {
    override val metricsService = new MockMetrics
    override val sessionCache = mockSessionCache
  }

  implicit val hc : HeaderCarrier = HeaderCarrier()

  case class TestModel(test: String)
  object TestModel {
    implicit val formats = Json.format[TestModel]
  }

  "Saving into KeyStore" should {
    "save the model" in {
      val testModel = TestModel("test")

      val returnCacheMap = CacheMap("", Map("" -> Json.toJson(testModel)))

      when(mockSessionCache.cache[TestModel](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(returnCacheMap))

      val result = connector.cache[TestModel](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      await(result) shouldBe returnCacheMap
    }
  }

  "Fetching and getting from KeyStore" should {
    "return a list" in {
      val testModel = TestModel("test")
      val list = List(testModel)

      when(mockSessionCache.fetchAndGetEntry[List[TestModel]](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(list)))

      val result = connector.fetchAndGet[List[TestModel]](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      await(result) shouldBe Some(list)
    }
  }

  "Fetching from KeyStore" should {
    "return a CacheMap" in {
      val testModel = TestModel("test")

      val returnCacheMap = CacheMap("", Map("" -> Json.toJson(testModel)))

      when(mockSessionCache.fetch()(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(returnCacheMap)))

      val result = connector.fetch()(ArgumentMatchers.any())
      await(result) shouldBe Some(returnCacheMap)
    }
  }

  "Removing from KeyStore" should {
    "return a HTTP Response" in {
      when(mockSessionCache.remove()(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = connector.remove()(ArgumentMatchers.any())
      await(result).status shouldBe HttpResponse(OK).status
    }
  }
}
