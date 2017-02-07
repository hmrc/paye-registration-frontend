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

import connectors._
import fixtures.{PAYERegistrationFixture, S4LFixture}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{BooleanFeatureSwitch, PAYEFeatureSwitch}

import scala.concurrent.Future


class AddressLookupServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture {

  implicit val hc = HeaderCarrier()

  val mockFeatureSwitch = mock[PAYEFeatureSwitch]
  val mockAddressLookupConnector = mock[AddressLookupConnector]

  class Setup {
    val service = new AddressLookupService (mockFeatureSwitch, mockAddressLookupConnector)
  }

  case class SetupWithProxy(boole: Boolean) {
    val service = new AddressLookupService (mockFeatureSwitch, mockAddressLookupConnector) {
      override def useAddressLookupFrontend = boole
    }
  }

  "Calling buildAddressLookupUrl" should {
    "return the Address Lookup frontend Url with payereg1 journey" in new SetupWithProxy(true) {
      await(service.buildAddressLookupUrl()) shouldBe "http://localhost:9028/lookup-address/uk/addresses/" +
        "payereg1" +
        "?continue=http://localhost:9870/register-for-paye/return-from-address"
    }

    "return the Address Lookup frontend Url with a custom tag" in new SetupWithProxy(true) {
      await(service.buildAddressLookupUrl("myNewTag")) shouldBe "http://localhost:9028/lookup-address/uk/addresses/" +
        "myNewTag" +
        "?continue=http://localhost:9870/register-for-paye/return-from-address"
    }

    "return the Address Lookup stub Url" in new SetupWithProxy(false) {
      await(service.buildAddressLookupUrl()) shouldBe "http://localhost:9870/register-for-paye/employing-staff"
    }
  }

  "Calling getAddress" should {
    "return an address" in new Setup {
      val mockAddress = Json.obj("x" -> "y")
      when(mockAddressLookupConnector.getAddress(Matchers.anyString())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(mockAddress))

      await(service.getAddress("123")) shouldBe mockAddress
    }
  }

  def feature(b: Boolean) = BooleanFeatureSwitch("addressLookup", enabled = b)

  "Calling useAddressLookupFrontend" should {
    "return true" in new Setup {
      when(mockFeatureSwitch.addressLookupFrontend).thenReturn(feature(true))
      service.useAddressLookupFrontend shouldBe true
    }
    "return false" in new Setup {
      when(mockFeatureSwitch.addressLookupFrontend).thenReturn(feature(false))
      service.useAddressLookupFrontend shouldBe false
    }
  }

}
