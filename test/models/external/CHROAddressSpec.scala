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

package models.external

import models.Address
import play.api.libs.json.{JsSuccess, Json}
import testHelpers.PAYERegSpec

class CHROAddressSpec extends PAYERegSpec {
  val tstCHROAddress = CHROAddress(
    premises = "14",
    addressLine1 = "Test Walker Street",
    addressLine2 = Some("Testley"),
    locality = "Testford",
    country = Some("UK"),
    poBox = None,
    postalCode = Some("TE1 1ST"),
    region = Some("Testshire")
  )

  val tstAddress = Address(
    line1 = "14 Test Walker Street",
    line2 = "Testley",
    line3 = Some("Testford"),
    line4 = Some("Testshire"),
    country = None,
    postCode = Some("TE1 1ST")
  )

  val tstCHROAddressJson = Json.parse(
    """{
      |  "premises":"14",
      |  "address_line_1":"Test Walker Street",
      |  "address_line_2":"Testley",
      |  "locality":"Testford",
      |  "country":"UK",
      |  "postal_code":"TE1 1ST",
      |  "region":"Testshire"
      |}""".stripMargin)

  "CHROAddress" should {
    "read from Json" in {
      Json.fromJson[CHROAddress](tstCHROAddressJson) shouldBe JsSuccess(tstCHROAddress)
    }
  }

  def testImplicitConversion(address: Address) = address

  "convertToAddress" should {
    "convert CHROAddress to Address" in {
      testImplicitConversion(tstCHROAddress) shouldBe tstAddress
    }

    "convert CHROAddress with a long first line" in {
      val testCHROAddress = CHROAddress(
        premises = "Unit 14234",
        addressLine1 = "Really Long Street Name",
        addressLine2 = Some("Testley"),
        locality = "Testford",
        country = None,
        poBox = None,
        postalCode = Some("TE1 1ST"),
        region = Some("Testshire")
      )

      val testAddress = Address(
        line1 = "Unit 14234",
        line2 = "Really Long Street Name",
        line3 = Some("Testley"),
        line4 = Some("Testford"),
        country = None,
        postCode = Some("TE1 1ST")
      )
      testImplicitConversion(testCHROAddress) shouldBe testAddress

    }

    "convert CHROAddress with a PO Box" in {
      val testCHROAddress = CHROAddress(
        premises = "Unit 14234",
        addressLine1 = "Really Long Street Name",
        addressLine2 = None,
        locality = "Testford",
        country = Some("UK"),
        poBox = Some("PO BOX TST36"),
        postalCode = Some("TE1 1ST"),
        region = Some("Testshire")
      )

      val testAddress = Address(
        line1 = "Unit 14234",
        line2 = "Really Long Street Name",
        line3 = Some("PO BOX TST36"),
        line4 = Some("Testford"),
        country = None,
        postCode = Some("TE1 1ST")
      )
      testImplicitConversion(testCHROAddress) shouldBe testAddress

    }

    "convert CHROAddress with a PO Box and short address line 1" in {
      val testCHROAddress = CHROAddress(
        premises = "12",
        addressLine1 = "Short Street Name",
        addressLine2 = None,
        locality = "Testford",
        country = Some("UK"),
        poBox = Some("PO BOX TST36"),
        postalCode = None,
        region = Some("Testshire")
      )

      val testAddress = Address(
        line1 = "12 Short Street Name",
        line2 = "PO BOX TST36",
        line3 = Some("Testford"),
        line4 = Some("Testshire"),
        country = Some("UK"),
        postCode = None
      )
      testImplicitConversion(testCHROAddress) shouldBe testAddress

    }

    "convert CHROAddress with a PO Box, a line 2 and short address line 1" in {
      val testCHROAddress = CHROAddress(
        premises = "12",
        addressLine1 = "Short Street Name",
        addressLine2 = Some("Industrial estate"),
        locality = "Testford",
        country = None,
        poBox = Some("PO BOX TST36"),
        postalCode = Some("TE1 1ST"),
        region = Some("Testshire")
      )

      val testAddress = Address(
        line1 = "12 Short Street Name",
        line2 = "Industrial estate PO BOX TST36",
        line3 = Some("Testford"),
        line4 = Some("Testshire"),
        country = None,
        postCode = Some("TE1 1ST")
      )
      testImplicitConversion(testCHROAddress) shouldBe testAddress

    }

    "convert CHROAddress with a PO Box, a line 2 and long address line 1" in {
      val testCHROAddress = CHROAddress(
        premises = "Unit 14234",
        addressLine1 = "Really Long Street Name",
        addressLine2 = Some("Industrial estate"),
        locality = "Testford",
        country = Some("UK"),
        poBox = Some("PO BOX TST36"),
        postalCode = Some("TE1 1ST"),
        region = Some("Testshire")
      )

      val testAddress = Address(
        line1 = "Unit 14234",
        line2 = "Really Long Street Name",
        line3 = Some("Industrial estate PO BOX TST36"),
        line4 = Some("Testford"),
        country = None,
        postCode = Some("TE1 1ST")
      )
      testImplicitConversion(testCHROAddress) shouldBe testAddress

    }

    "convert CHROAddress with minimal data" in {
      val testCHROAddress = CHROAddress(
        premises = "12",
        addressLine1 = "Short Street Name",
        addressLine2 = None,
        locality = "Testford",
        country = Some("UK"),
        poBox = None,
        postalCode = Some("TE1 1ST"),
        region = None
      )

      val testAddress = Address(
        line1 = "12 Short Street Name",
        line2 = "Testford",
        line3 = None,
        line4 = None,
        country = None,
        postCode = Some("TE1 1ST")
      )
      testImplicitConversion(testCHROAddress) shouldBe testAddress

    }
  }
}
