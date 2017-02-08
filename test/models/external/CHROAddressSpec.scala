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

import fixtures.CoHoAPIFixture
import models.view.Address
import play.api.libs.json.{JsSuccess, Json}
import testHelpers.PAYERegSpec

class CHROAddressSpec extends PAYERegSpec {
  val tstCHROAddress = CHROAddress(
    premises = "14",
    addressLine1 = "St Test Walker",
    addressLine2 = Some("Testley"),
    locality = "Testford",
    country = Some("UK"),
    poBox = None,
    postalCode = Some("TE1 1ST"),
    region = Some("Testshire")
  )

  val tstAddress = Address(
    line1 = "14 St Test Walker",
    line2 = "Testford",
    line3 = Some("Testley"),
    line4 = None,
    country = Some("UK"),
    postCode = Some("TE1 1ST")
  )

  val tstCHROAddressJson = Json.parse(
    """{
      |  "premises":"14",
      |  "address_line_1":"St Test Walker",
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

  "convertToAddress" should {
    "convert CHROAddress to Address" in {
      def testImplicitConversion(address: Address) = address
      testImplicitConversion(tstCHROAddress) shouldBe tstAddress
    }
  }
}
