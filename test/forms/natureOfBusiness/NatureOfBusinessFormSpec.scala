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

package forms.natureOfBusiness

import forms.natureOfBuinessDetails.NatureOfBusinessForm
import models.view.NatureOfBusiness
import play.api.data.FormError
import play.api.i18n.MessagesApi
import testHelpers.PAYERegSpec

class NatureOfBusinessFormSpec extends PAYERegSpec {

  val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val testForm = NatureOfBusinessForm.form
  val validData = Map(
    "description" -> "I am a test description"
  )

  val invalidDataNoEntry = Map(
    "description" -> ""
  )

  val invalidData100Chars = Map(
    "description" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  )

  val invalidDataTooLong = Map(
    "description" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  )

  val dataWithNewlineTab = Map(
    "description" -> "\n\n\nI\nam\ta\r\ntest\rdescription\n\n\n\n\n"
  )

  val invalidDataChars = Map(
    "description" -> "I @m @ t£st descr!ption"
  )

  "Validating the NatureOfBusiness form" should {
    "return the original form if there are no errors" in {
      val result = NatureOfBusinessForm.form.bind(validData).fold(
        errors => errors,
        success => success
      )

      result shouldBe NatureOfBusiness("I am a test description")
    }

    "return a trimmed and newline/tab replaced by whitespace if there are no errors" in {
      val result = NatureOfBusinessForm.form.bind(dataWithNewlineTab).fold(
        errors => errors,
        success => success
      )

      result shouldBe NatureOfBusiness("I am a test description")
    }

    "return a form with errors" when {
      "the description is empty" in {
        val boundForm = testForm.bind(invalidDataNoEntry)
        val formError = FormError("description", "errors.invalid.sic.noEntry")

        boundForm.errors shouldBe Seq(formError)
      }

      "the description is equal to 100 chars" in {
        val boundForm = testForm.bind(invalidData100Chars)
        val formError = FormError("description", "errors.invalid.sic.overCharLimit")

        boundForm.errors shouldBe Seq(formError)
      }

      "the description is over 100 chars" in {
        val boundForm = testForm.bind(invalidDataTooLong)
        val formError = FormError("description", "errors.invalid.sic.overCharLimit")

        boundForm.errors shouldBe Seq(formError)
      }

      "the description contains invalid chars" in {
        val boundForm = testForm.bind(invalidDataChars)
        val formError = FormError("description", "errors.invalid.sic.invalidChars")

        boundForm.errors shouldBe Seq(formError)
      }
    }
  }
}
