/*
 * Copyright 2021 HM Revenue & Customs
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

import config.PAYEShortLivedCache
import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.view.{TradingName => TradingNameView}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

class S4LConnectorSpec extends PayeComponentSpec {

  val mockShortLivedCache = mock[PAYEShortLivedCache]

  val S4LConnectorTest = new S4LConnector {
    override val metricsService = new MockMetrics
    override val shortCache = mockShortLivedCache
    override val successCounter = metricsService.s4lSuccessResponseCounter
    override val failedCounter = metricsService.s4lFailedResponseCounter
    override val emptyResponseCounter = metricsService.s4lEmptyResponseCounter

    override def timer = metricsService.s4lResponseTimer.time()
    override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  }

  val tNameModel = TradingNameView(differentName = true, Some("Tradez R Us"))
  val cacheMap = CacheMap("", Map("" -> Json.toJson(tNameModel)))

  "Fetching from save4later" should {
    "return the correct model" in {
      when(mockShortLivedCache.fetchAndGetEntry[TradingNameView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Option(tNameModel)))

      val result = S4LConnectorTest.fetchAndGet[TradingNameView]("", "")
      await(result) mustBe Some(tNameModel)
    }
  }

  "Saving a model into save4later" should {
    "save the model" in {
      val returnCacheMap = CacheMap("", Map("" -> Json.toJson(tNameModel)))

      when(mockShortLivedCache.cache[TradingNameView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(returnCacheMap))

      val result = S4LConnectorTest.saveForm[TradingNameView]("", "", tNameModel)
      await(result) mustBe returnCacheMap
    }
  }

  "clearing an entry using save4later" should {
    "clear the entry given the user id" in {
      when(mockShortLivedCache.remove(ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = S4LConnectorTest.clear("test")
      await(result).status mustBe HttpResponse(OK).status
    }
  }

  "fetchAll" should {
    "fetch all entries in S4L" in {
      when(mockShortLivedCache.fetch(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      val result = S4LConnectorTest.fetchAll("testUserId")
      await(result).get mustBe cacheMap
    }

    "return an Upstream response when a failure occurs on the fetch (and then returned from metrics)" in {
      when(mockShortLivedCache.fetch(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("400", 400, 400)))

      intercept[Upstream4xxResponse](await(S4LConnectorTest.fetchAll("testUserId")))
    }
  }
}
