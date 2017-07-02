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

package services

import connectors.S4LConnect
import fixtures.{KeystoreFixture, PAYERegistrationFixture}
import models.view.{TradingName => TradingNameView}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

class S4LServiceSpec extends PAYERegSpec with KeystoreFixture with PAYERegistrationFixture {

  trait Setup {
    val service = new S4LSrv {
      override val s4LConnector: S4LConnect = mockS4LConnector
    }
  }

  implicit val hc = new HeaderCarrier()

  val tstTradingNameModel = TradingNameView(differentName = false, tradingName = None)

  "S4L Service" should {

    "save a form with the correct key" in new Setup {
      mockS4LSaveForm[TradingNameView]("tradingName", CacheMap("t-name", Map.empty))

      await(service.saveForm[TradingNameView]("tradingName", tstTradingNameModel, "regId")).id shouldBe "t-name"
    }

    "fetch a form with the correct key" in new Setup {
      mockS4LFetchAndGet[TradingNameView]("tradingName2", Some(tstTradingNameModel))

      await(service.fetchAndGet[TradingNameView]("tradingName2", "regId")) shouldBe Some(tstTradingNameModel)
    }

    "save a Map with the correct key" in new Setup {
      mockS4LSaveForm[Map[Int, String]]("intMap", CacheMap("int-map", Map.empty))

      await(service.saveMap[Int, String]("intMap", Map(1 -> "string", 2 -> "otherString"), "regId")).id shouldBe "int-map"
    }

    "fetch a Map with the correct key" in new Setup {
      val map = Map("one" -> 1, "two" -> 2)
      mockS4LFetchAndGet[Map[String, Int]]("stringMap", Some(map))

      await(service.fetchAndGetMap[String, Int]("stringMap", "regId")) shouldBe Some(map)
    }

    "clear down S4L data" in new Setup {
      mockS4LClear()

      await(service.clear("regId")).status shouldBe 200
    }

    "fetch all data" in new Setup {
      mockS4LFetchAll(Some(CacheMap("allData", Map.empty)))

      await(service.fetchAll("regId")) shouldBe Some(CacheMap("allData", Map.empty))
    }

  }

}
