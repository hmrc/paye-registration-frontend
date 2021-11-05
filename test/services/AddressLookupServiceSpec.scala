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

import helpers.mocks.MockMetrics
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.Address
import models.external.AlfJourneyConfig
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

import java.util.Locale
import scala.concurrent.Future


class AddressLookupServiceSpec extends PayeComponentSpec with PayeFakedApp {

  implicit val mockMessages = mockMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))
  val metricsMock = new MockMetrics

  class Setup {
    val service = new AddressLookupService(
      addressLookupConnector = mockAddressLookupConnector,
      addressLookupConfigBuilderService = mockAddressLookupConfigBuilderServiceMock
    )
  }

  "Calling buildAddressLookupUrl" should {
    "return the Address Lookup frontend Url with payereg1 journey" in new Setup {
      when(mockAddressLookupConnector.getOnRampUrl(ArgumentMatchers.any[AlfJourneyConfig]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("test-url"))

      await(service.buildAddressLookupUrl("payereg1", Call("GET", "/register-for-paye/test-url"))) mustBe "test-url"
    }
  }

  "Calling getAddress" should {
    "return an address" in new Setup {
      val expected =
        Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        )

      when(mockAddressLookupConnector.getAddress(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      await(service.getAddress("1234567890")) mustBe expected
    }
  }
}
