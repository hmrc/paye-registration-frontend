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
import fixtures.{PAYERegistrationFixture, KeystoreFixture}
import models.currentProfile.CurrentProfile
import models.formModels.TradingNameFormModel
import models.payeRegistration.companyDetails.TradingName
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

class S4LServiceSpec extends PAYERegSpec with KeystoreFixture with PAYERegistrationFixture {

  trait Setup {
    val service = new S4LService {
      override val s4LConnector = mockS4LConnector
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  implicit val hc = new HeaderCarrier()

  val tstTradingNameModel = TradingNameFormModel(tradeUnderDifferentName = "no", tradingName = None)

  "S4L Service" should {

    "save a form with the correct key" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LSaveForm[TradingNameFormModel]("tradingName", CacheMap("t-name", Map.empty))

      await(service.saveForm[TradingNameFormModel]("tradingName", tstTradingNameModel)).id shouldBe "t-name"
    }

    "fetch a form with the correct key" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LFetchAndGet[TradingNameFormModel]("tradingName2", Some(tstTradingNameModel))

      await(service.fetchAndGet[TradingNameFormModel]("tradingName2")) shouldBe Some(tstTradingNameModel)
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

    "save a full PAYE Registration" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      mockS4LSaveForm[TradingName](CacheKeys.TradingName.toString, CacheMap("t-name", Map.empty))

      await(service.saveRegistration(validPAYERegistration)) shouldBe validPAYERegistration
    }

    "save a PAYE Registration with no company details" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))

      val noDetailsReg = validPAYERegistration.copy(companyDetails = None)
      await(service.saveRegistration(noDetailsReg)) shouldBe noDetailsReg
    }

  }

}
