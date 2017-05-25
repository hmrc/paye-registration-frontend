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
import mocks.MockMetrics
import models.Address
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.Call
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{BooleanFeatureSwitch, PAYEFeatureSwitch}

import scala.concurrent.Future


class AddressLookupServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture with ServicesConfig {

  implicit val hc = HeaderCarrier()

  val mockFeatureSwitch = mock[PAYEFeatureSwitch]
  val mockAddressLookupConnector = mock[AddressLookupConnector]
  val mockMetrics = mock[MetricsService]
  val metricsMock = new MockMetrics

  class Setup {
    val service = new AddressLookupService(mockFeatureSwitch, mockAddressLookupConnector, mockMetrics) {
      override val metricsService = metricsMock
    }
  }

  case class SetupWithProxy(boole: Boolean) {
    val service = new AddressLookupService(mockFeatureSwitch, mockAddressLookupConnector, mockMetrics) {
      override val metricsService = metricsMock
      override def useAddressLookupFrontend = boole
    }
  }

  "Calling buildAddressLookupUrl" should {
    "return the Address Lookup frontend Url with payereg1 journey" in new SetupWithProxy(true) {
      when(mockAddressLookupConnector.getOnRampUrl(ArgumentMatchers.contains("payereg1"), ArgumentMatchers.any[Call]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("test-url"))

      await(service.buildAddressLookupUrl("payereg1", Call("GET", "/register-for-paye/test-url"))) shouldBe "test-url"
    }

    "return the Address Lookup stub Url for PPOB" in new SetupWithProxy(false) {
      await(service.buildAddressLookupUrl("payreg1", Call("GET","/return-from-address-for-ppob"))) shouldBe "/no-lookup-ppob-address"
    }

    "return the Address Lookup stub Url for PAYE Contact" in new SetupWithProxy(false) {
      await(service.buildAddressLookupUrl("payreg1", Call("GET","/return-from-address-for-corresp-addr"))) shouldBe "/no-lookup-correspondence-address"
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

      implicit val request = FakeRequest("GET", "/test-uri?id=1234567890")

      await(service.getAddress) shouldBe Some(expected)
    }

    "return none" in new Setup {
      implicit val request = FakeRequest("GET", "/test-uri")

      await(service.getAddress) shouldBe None
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
