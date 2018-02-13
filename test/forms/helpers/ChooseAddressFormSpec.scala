/*
 * Copyright 2018 HM Revenue & Customs
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

import helpers.PayeComponentSpec
import models.view._
import play.api.data.FormError

class ChooseAddressFormSpec extends PayeComponentSpec {
  object testForm extends ChooseAddressForm {
    override val errMessage = "tstErrorMessage"
  }

  "Binding ChooseAddressForm" when {
    "Supplied with full data for value other" should {
      val data = Map(
        "chosenAddress" -> "other"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Right(Other)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", Other) mustBe Map("tstKey" -> "other")
      }
    }

    "Supplied with full data for value roAddress" should {
      val data = Map(
        "chosenAddress" -> "roAddress"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Right(ROAddress)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", ROAddress) mustBe Map("tstKey" -> "roAddress")
      }
    }

    "Supplied with full data for value ppobAddress" should {
      val data = Map(
        "chosenAddress" -> "ppobAddress"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Right(PPOBAddress)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", PPOBAddress) mustBe Map("tstKey" -> "ppobAddress")
      }
    }

    "Supplied with full data for value correspondenceAddress" should {
      val data = Map(
        "chosenAddress" -> "correspondenceAddress"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Right(CorrespondenceAddress)
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", CorrespondenceAddress) mustBe Map("tstKey" -> "correspondenceAddress")
      }
    }

    "Supplied with full data for value prepopAddress10" should {
      val data = Map(
        "chosenAddress" -> "prepopAddress10"
      )

      "Bind successfully" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Right(PrepopAddress(10))
      }

      "Unbind successfully" in {
        testForm.addressChoiceFormatter.unbind("tstKey", PrepopAddress(10)) mustBe Map("tstKey" -> "prepopAddress10")
      }
    }

    "Supplied with wrong data" should {
      val data = Map(
        "chosenAddress" -> "prepopAddressxx"
      )

      "Fail to bind with the correct errors" in {
        a[ConvertToPrepopAddressException] mustBe thrownBy(testForm.addressChoiceFormatter.bind("chosenAddress", data))
      }
    }

    "Supplied with no data" should {
      val data = Map(
        "chosenAddress" -> ""
      )

      "Fail to bind with the correct errors" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Left(List(FormError("chosenAddress",List("tstErrorMessage"),Nil)))
      }
    }

    "Supplied with no data and a csrf token" should {
      val data = Map(
        "csrfToken" -> "CSRFCSRF",
        "chosenAddress" -> ""
      )

      "Fail to bind with the correct errors" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Left(List(FormError("chosenAddress",List("tstErrorMessage"),Nil)))
      }
    }

    "Supplied with no corresponding data key" should {
      val data = Map(
        "chosenAddress2" -> ""
      )

      "Fail to bind with the correct errors" in {
        testForm.addressChoiceFormatter.bind("chosenAddress", data) mustBe Left(List(FormError("chosenAddress",List("tstErrorMessage"),Nil)))
      }
    }
  }
}
