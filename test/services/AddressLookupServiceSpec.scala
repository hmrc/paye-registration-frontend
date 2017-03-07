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
import models.Address
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
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
      await(service.buildAddressLookupUrl("payereg1", Call("GET", "/register-for-paye/test-url"))) shouldBe "http://localhost:9028/lookup-address/uk/addresses/" +
        "payereg1" +
        "?continue=http://localhost:9870/register-for-paye/test-url"
    }

    "return the Address Lookup frontend Url with a custom tag" in new SetupWithProxy(true) {
      await(service.buildAddressLookupUrl("myNewTag", Call("GET", "/register-for-paye/test-url"))) shouldBe "http://localhost:9028/lookup-address/uk/addresses/" +
        "myNewTag" +
        "?continue=http://localhost:9870/register-for-paye/test-url"
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
      val mockAddress = Json.parse(
        """
          |{
          |   "id" : "testId",
          |   "address" : {
          |     "lines" : [
          |       "L1",
          |       "L2",
          |       "L3",
          |       "L4"
          |     ],
          |     "postcode" : "testPostcode",
          |     "country" : {
          |       "code" : "testCode",
          |       "name" : "testName"
          |     }
          |   }
          |}
        """.stripMargin
      ).as[JsObject]

      val expected =
        Some(Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        ))

      when(mockAddressLookupConnector.getAddress(Matchers.anyString())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(mockAddress))

      implicit val request = FakeRequest("GET", "/test-uri?id=1234567890")

      await(service.getAddress) shouldBe expected
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

  "jsonToAddress" should {
    "convert an address with all four lines present" in new Setup {
      val jsObject = Json.parse(
        """
          |{
          |   "id" : "testId",
          |   "address" : {
          |     "lines" : [
          |       "L1",
          |       "L2",
          |       "L3",
          |       "L4"
          |     ],
          |     "postcode" : "testPostcode",
          |     "country" : {
          |       "code" : "testCode",
          |       "name" : "testName"
          |     }
          |   }
          |}
        """.stripMargin
      ).as[JsObject]

      val address =
        Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        )

      val result = service.jsonToAddress(jsObject)
      result shouldBe address
    }

    "convert an address with only three lines present" in new Setup {
      val jsObject = Json.parse(
        """
          |{
          |   "id" : "testId",
          |   "address" : {
          |     "lines" : [
          |       "L1",
          |       "L2",
          |       "L3"
          |     ],
          |     "postcode" : "testPostcode",
          |     "country" : {
          |       "code" : "testCode",
          |       "name" : "testName"
          |     }
          |   }
          |}
        """.stripMargin
      ).as[JsObject]

      val address =
        Address(
          "L1",
          "L2",
          Some("L3"),
          None,
          Some("testPostcode"),
          Some("testCode")
        )

      val result = service.jsonToAddress(jsObject)
      result shouldBe address
    }

    "convert an address with only two lines present" in new Setup {
      val jsObject = Json.parse(
        """
          |{
          |   "id" : "testId",
          |   "address" : {
          |     "lines" : [
          |       "L1",
          |       "L2"
          |     ],
          |     "postcode" : "testPostcode",
          |     "country" : {
          |       "code" : "testCode",
          |       "name" : "testName"
          |     }
          |   }
          |}
        """.stripMargin
      ).as[JsObject]

      val address =
        Address(
          "L1",
          "L2",
          None,
          None,
          Some("testPostcode"),
          Some("testCode")
        )

      val result = service.jsonToAddress(jsObject)
      result shouldBe address
    }
  }
}
