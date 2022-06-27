/*
 * Copyright 2022 HM Revenue & Customs
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

import helpers.PayeComponentSpec

class ChosenAddressSpec extends PayeComponentSpec {
  val tstChosenAddressOther = ChosenAddress(chosenAddress = Other)
  val tstChosenAddressROAddress = ChosenAddress(chosenAddress = ROAddress)
  val tstChosenAddressPPOBAddress = ChosenAddress(chosenAddress = PPOBAddress)
  val tstChosenAddressCorrespondenceAddress = ChosenAddress(chosenAddress = CorrespondenceAddress)

  def tstChosenAddressPrepopAddress(index: Int) = ChosenAddress(chosenAddress = PrepopAddress(index))

  "AddressChoice fromString" should {
    "return a Other object when the input string is other" in {
      AddressChoice.fromString("otherAddress") mustBe Other
    }

    "return a ROAddress object when the input string is roAddress" in {
      AddressChoice.fromString("roAddress") mustBe ROAddress
    }

    "return a PPOBAddress object when the input string is ppobAddress" in {
      AddressChoice.fromString("ppobAddress") mustBe PPOBAddress
    }

    "return a CorrespondenceAddress object when the input string is correspondenceAddress" in {
      AddressChoice.fromString("correspondenceAddress") mustBe CorrespondenceAddress
    }

    "return a PrepopAddress object with index 0 when the input string is prepopAddress0" in {
      AddressChoice.fromString("prepopAddress0") mustBe PrepopAddress(0)
    }

    "return a PrepopAddres object with index 10 when the input string is prepopAddress10" in {
      AddressChoice.fromString("prepopAddress10") mustBe PrepopAddress(10)
    }

    "return an exception when the input string is not correct" in {
      a[ConvertToPrepopAddressException] mustBe thrownBy(AddressChoice.fromString("Other"))
    }

    "return an exception when the input string is not a valid PrepopAddress index value" in {
      a[ConvertToPrepopAddressException] mustBe thrownBy(AddressChoice.fromString("prepopAddressx"))
    }
  }

  "ROAddress toString" should {
    "return roAddress string" in {
      ROAddress.toString mustBe "roAddress"
    }
  }

  "PPOBAddress toString" should {
    "return ppobAddress string" in {
      PPOBAddress.toString mustBe "ppobAddress"
    }
  }

  "CorrespondenceAddress toString" should {
    "return correspondenceAddress string" in {
      CorrespondenceAddress.toString mustBe "correspondenceAddress"
    }
  }

  "Other toString" should {
    "return other string" in {
      Other.toString mustBe "other"
    }
  }

  "PrepopAddress toString" should {
    "return prepopAddress0 string" in {
      PrepopAddress(0).toString mustBe "prepopAddress0"
    }

    "return prepopAddress10 string" in {
      PrepopAddress(10).toString mustBe "prepopAddress10"
    }
  }
}
