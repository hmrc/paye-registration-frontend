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

package forms.helpers

import models.view.{AddressChoice, ChosenAddress}
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class ChooseAddressFormSpec extends UnitSpec {
  object testForm extends ChooseAddressForm {
    override val errMessage = "tstErrorMessage"
  }

  "Binding ChooseAddressForm" when {
    "Supplied with full data for value other" should {
      val data = Map(
        "chosenAddress" -> "other"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Right(AddressChoice.other)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", AddressChoice.other) shouldBe Map("tstKey" -> "other")
      }
    }

    "Supplied with full data for value roAddress" should {
      val data = Map(
        "chosenAddress" -> "roAddress"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Right(AddressChoice.roAddress)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", AddressChoice.roAddress) shouldBe Map("tstKey" -> "roAddress")
      }
    }

    "Supplied with full data for value ppobAddress" should {
      val data = Map(
        "chosenAddress" -> "ppobAddress"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Right(AddressChoice.ppobAddress)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", AddressChoice.ppobAddress) shouldBe Map("tstKey" -> "ppobAddress")
      }
    }

    "Supplied with full data for value correspondenceAddress" should {
      val data = Map(
        "chosenAddress" -> "correspondenceAddress"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Right(AddressChoice.correspondenceAddress)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", AddressChoice.correspondenceAddress) shouldBe Map("tstKey" -> "correspondenceAddress")
      }
    }

    "Supplied with no data" should {
      val data = Map(
        "chosenAddress" -> ""
      )

      "Fail to bind with the correct errors" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Left(List(FormError("chosenAddress",List("tstErrorMessage"),Nil)))
      }
    }

    "Supplied with no data and a csrf token" should {
      val data = Map(
        "csrfToken" -> "CSRFCSRF",
        "chosenAddress" -> ""
      )

      "Fail to bind with the correct errors" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Left(List(FormError("chosenAddress",List("tstErrorMessage"),Nil)))
      }
    }

    "Supplied with no corresponding data key" should {
      val data = Map(
        "chosenAddress2" -> ""
      )

      "Fail to bind with the correct errors" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) shouldBe Left(List(FormError("chosenAddress",List("tstErrorMessage"),Nil)))
      }
    }
  }
}
