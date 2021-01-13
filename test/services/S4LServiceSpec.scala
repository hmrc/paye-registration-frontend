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

package services

import helpers.PayeComponentSpec
import models.view.{TradingName => TradingNameView}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class S4LServiceSpec extends PayeComponentSpec {

  trait Setup {
    val service = new S4LService {
      override val s4LConnector = mockS4LConnector
    }
  }

  val tstTradingNameModel = TradingNameView(differentName = false, tradingName = None)

  "S4L Service" should {

    "save a form with the correct key" in new Setup {
      when(mockS4LConnector.saveForm[TradingNameView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("t-name", Map.empty)))

      await(service.saveForm[TradingNameView]("tradingName", tstTradingNameModel, "regId")).id mustBe "t-name"
    }

    "fetch a form with the correct key" in new Setup {
      when(mockS4LConnector.fetchAndGet[TradingNameView](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(tstTradingNameModel)))

      await(service.fetchAndGet[TradingNameView]("tradingName2", "regId")) mustBe Some(tstTradingNameModel)
    }

    "save a Map with the correct key" in new Setup {
      when(mockS4LConnector.saveForm[Map[Int, String]](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("int-map", Map.empty)))

      await(service.saveIntMap[String]("intMap", Map(1 -> "string", 2 -> "otherString"), "regId")).id mustBe "int-map"
    }

    "fetch a Map with the correct key" in new Setup {
      val map = Map("one" -> 1, "two" -> 2)

      when(mockS4LConnector.fetchAndGet[Map[String, Int]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(map)))

      await(service.fetchAndGetIntMap[Int]("stringMap", "regId")) mustBe Some(map)
    }

    "clear down S4L data" in new Setup {
      when(mockS4LConnector.clear(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(HttpResponse(200)))

      await(service.clear("regId")).status mustBe 200
    }

    "fetch all data" in new Setup {
      when(mockS4LConnector.fetchAll(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(CacheMap("allData", Map.empty))))

      await(service.fetchAll("regId")) mustBe Some(CacheMap("allData", Map.empty))
    }
  }
}
