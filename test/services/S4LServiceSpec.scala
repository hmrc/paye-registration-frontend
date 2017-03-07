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

import enums.CacheKeys
import fixtures.{KeystoreFixture, PAYERegistrationFixture}
import models.external.CurrentProfile
import models.view.{TradingName => TradingNameView}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

class S4LServiceSpec extends PAYERegSpec with KeystoreFixture with PAYERegistrationFixture {

  trait Setup {
    val service = new S4LService (mockS4LConnector, mockKeystoreConnector)
  }

  implicit val hc = new HeaderCarrier()

  val tstTradingNameModel = TradingNameView(differentName = false, tradingName = None)

  "S4L Service" should {

    "save a form with the correct key" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LSaveForm[TradingNameView]("tradingName", CacheMap("t-name", Map.empty))

      await(service.saveForm[TradingNameView]("tradingName", tstTradingNameModel)).id shouldBe "t-name"
    }

    "fetch a form with the correct key" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LFetchAndGet[TradingNameView]("tradingName2", Some(tstTradingNameModel))

      await(service.fetchAndGet[TradingNameView]("tradingName2")) shouldBe Some(tstTradingNameModel)
    }

    "clear down S4L data" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LClear()

      await(service.clear()).status shouldBe 200
    }

    "fetch all data" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LFetchAll(Some(CacheMap("allData", Map.empty)))

      await(service.fetchAll()) shouldBe Some(CacheMap("allData", Map.empty))
    }

  }

}
