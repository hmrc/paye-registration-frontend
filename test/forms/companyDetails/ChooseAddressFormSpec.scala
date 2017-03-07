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

package forms.companyDetails

import forms.ChooseAddressForm
import models.view.{AddressChoice, ChosenAddress}
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class ChooseAddressFormSpec extends UnitSpec {
  val testForm = ChooseAddressForm.form

  "Binding ChooseAddressForm to a model" when {
    "Supplied with full data for value other" should {
      val data = Map(
        "chosenAddress" -> "other"
      )
      val model = ChosenAddress(chosenAddress = AddressChoice.other)

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel shouldBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data shouldBe Map(
          "chosenAddress" -> "other"
        )
      }
    }

    "Supplied with full data for value roAddress" should {
      val data = Map(
        "chosenAddress" -> "roAddress"
      )
      val model = ChosenAddress(chosenAddress = AddressChoice.roAddress)

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel shouldBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data shouldBe Map(
          "chosenAddress" -> "roAddress"
        )
      }
    }

    "Supplied with full data for value ppobAddress" should {
      val data = Map(
        "chosenAddress" -> "ppobAddress"
      )
      val model = ChosenAddress(chosenAddress = AddressChoice.ppobAddress)

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel shouldBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data shouldBe Map(
          "chosenAddress" -> "ppobAddress"
        )
      }
    }

    "Supplied with no data" should {
      val data = Map(
        "chosenAddress" -> ""
      )

      "Fail to bind with the correct errors" in {
        val boundForm = testForm.bind(data).fold(
          errors => errors,
          success => testForm.fill(success)
        )
        boundForm.errors shouldBe Seq(FormError("chosenAddress", "errors.invalid.addressChoice.noEntry"))
        boundForm.data shouldBe data
      }
    }

    "Supplied with no data and a csrf token" should {
      val data = Map(
        "csrfToken" -> "CSRFCSRF",
        "chosenAddress" -> ""
      )

      "Fail to bind with the correct errors" in {
        val boundForm = testForm.bind(data).fold(
          errors => errors,
          success => testForm.fill(success)
        )
        boundForm.errors shouldBe Seq(FormError("chosenAddress", "errors.invalid.addressChoice.noEntry"))
        boundForm.data shouldBe data
      }
    }
  }
}
