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

package models.view

import play.api.libs.json.{JsSuccess, Json}
import testHelpers.PAYERegSpec

class ChosenAddressSpec extends PAYERegSpec {
  val tstChosenAddressOther = ChosenAddress(chosenAddress = AddressChoice.other)
  val tstChosenAddressROAddress = ChosenAddress(chosenAddress = AddressChoice.roAddress)
  val tstChosenAddressPPOBAddress = ChosenAddress(chosenAddress = AddressChoice.ppobAddress)

  val tstChosenAddressOtherJson = Json.parse(
    """{
      |  "chosenAddress":"other"
      |}""".stripMargin
  )

  val tstChosenAddressROAddressJson = Json.parse(
    """{
      |  "chosenAddress":"roAddress"
      |}""".stripMargin
  )

  val tstChosenAddressPPOBAddressJson = Json.parse(
    """{
      |  "chosenAddress":"ppobAddress"
      |}""".stripMargin
  )

  val tstWrongChosenAddressJson = Json.parse(
    """{
      |  "chosenAddress":"wrong"
      |}""".stripMargin
  )

  "ChosenAddress" should {
    "read from Json with other value" in {
      Json.fromJson[ChosenAddress](tstChosenAddressOtherJson).asOpt shouldBe Some(tstChosenAddressOther)
    }

    "read from Json with roAddress value" in {
      Json.fromJson[ChosenAddress](tstChosenAddressROAddressJson).asOpt shouldBe Some(tstChosenAddressROAddress)
    }

    "read from Json with ppobAddress value" in {
      Json.fromJson[ChosenAddress](tstChosenAddressPPOBAddressJson).asOpt shouldBe Some(tstChosenAddressPPOBAddress)
    }

    "read from Json with a wrong value " in {
      Json.fromJson[ChosenAddress](tstWrongChosenAddressJson).asOpt shouldBe None
    }

    "write to json with other value" in {
      Json.toJson[ChosenAddress](tstChosenAddressOther) shouldBe tstChosenAddressOtherJson
    }

    "write to json with roAddress value" in {
      Json.toJson[ChosenAddress](tstChosenAddressROAddress) shouldBe tstChosenAddressROAddressJson
    }

    "write to json with ppobAddress value" in {
      Json.toJson[ChosenAddress](tstChosenAddressPPOBAddress) shouldBe tstChosenAddressPPOBAddressJson
    }
  }
}
