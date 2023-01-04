/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.test

import helpers.PayeComponentSpec
import models.api.PAYEContact
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.data.FormError

class TestPAYEContactFormSpec extends PayeComponentSpec {
  val testForm = TestPAYEContactForm.form

  "Binding TestPAYEContactForm to a model" when {
    "Bind successfully with full data" should {
      val data = Map(
        "payeContactDetails.name" -> "tata",
        "payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContactDetails.digitalContactDetails.phoneNumber" -> "0998654",
        "correspondenceAddress.line1" -> "Testing Bld",
        "correspondenceAddress.line2" -> "1 Test Street",
        "correspondenceAddress.line3" -> "Testshire",
        "correspondenceAddress.line4" -> "Testford",
        "correspondenceAddress.postCode" -> "TE1 1ST",
        "correspondenceAddress.country" -> "UK"
      )

      val model = PAYEContact(
        contactDetails = PAYEContactDetails(
          name = "tata",
          digitalContactDetails = DigitalContactDetails(
            email = Some("tata@test.com"),
            mobileNumber = Some("123456"),
            phoneNumber = Some("0998654")
          )
        ),
        correspondenceAddress = Address(
          line1 = "Testing Bld",
          line2 = "1 Test Street",
          line3 = Some("Testshire"),
          line4 = Some("Testford"),
          postCode = Some("TE1 1ST"),
          country = Some("UK")
        )
      )

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel mustBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data mustBe Map(
          "payeContactDetails.name" -> "tata",
          "payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
          "payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
          "payeContactDetails.digitalContactDetails.phoneNumber" -> "0998654",
          "correspondenceAddress.line1" -> "Testing Bld",
          "correspondenceAddress.line2" -> "1 Test Street",
          "correspondenceAddress.line3" -> "Testshire",
          "correspondenceAddress.line4" -> "Testford",
          "correspondenceAddress.postCode" -> "TE1 1ST",
          "correspondenceAddress.country" -> "UK"
        )
      }
    }

    "Bind successfully with not full data" should {
      val data = Map(
        "payeContactDetails.name" -> "tata",
        "payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContactDetails.digitalContactDetails.phoneNumber" -> "",
        "correspondenceAddress.line1" -> "Testing Bld",
        "correspondenceAddress.line2" -> "1 Test Street",
        "correspondenceAddress.line3" -> "",
        "correspondenceAddress.line4" -> "",
        "correspondenceAddress.postCode" -> "TE1 1ST",
        "correspondenceAddress.country" -> ""
      )

      val model = PAYEContact(
        contactDetails = PAYEContactDetails(
          name = "tata",
          digitalContactDetails = DigitalContactDetails(
            email = Some("tata@test.com"),
            mobileNumber = Some("123456"),
            phoneNumber = None
          )
        ),
        correspondenceAddress = Address(
          line1 = "Testing Bld",
          line2 = "1 Test Street",
          line3 = None,
          line4 = None,
          postCode = Some("TE1 1ST"),
          country = None
        )
      )

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel mustBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data mustBe Map(
          "payeContactDetails.name" -> "tata",
          "payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
          "payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
          "correspondenceAddress.line1" -> "Testing Bld",
          "correspondenceAddress.line2" -> "1 Test Street",
          "correspondenceAddress.postCode" -> "TE1 1ST"
        )
      }
    }
  }

  "Have the correct error" when {
    "paye contact name is not completed" in {
      val data: Map[String, String] = Map(
        "payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "correspondenceAddress.line1" -> "Testing Bld",
        "correspondenceAddress.line2" -> "1 Test Street",
        "correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val payeContactNameError = FormError("payeContactDetails.name", "error.required")

      boundForm.errors mustBe Seq(payeContactNameError)
      boundForm.data mustBe data
    }

    "correspondenceAddress line1 and line2 are not completed" in {
      val data: Map[String, String] = Map(
        "payeContactDetails.name" -> "tata",
        "payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val formErrorCorrespondenceAddressLine1 = FormError("correspondenceAddress.line1", "error.required")
      val formErrorCorrespondenceAddressLine2 = FormError("correspondenceAddress.line2", "error.required")

      boundForm.error("correspondenceAddress.line1") mustBe Some(formErrorCorrespondenceAddressLine1)
      boundForm.error("correspondenceAddress.line2") mustBe Some(formErrorCorrespondenceAddressLine2)
      boundForm.data mustBe data
    }
  }
}
