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

package forms.test

import models.{Address, DigitalContactDetails}
import models.api.CompanyDetails
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class TestPAYERegCompanyDetailsSetupFormSpec extends UnitSpec {
  val testForm = TestPAYERegCompanyDetailsSetupForm.form

  "Binding TestPAYERegCompanyDetailsSetupForm to a model" when {
    "Bind successfully with full data" should {
      val data = Map(
        "crn" -> "abc",
        "companyName" -> "TEST LTD",
        "tradingName" -> "NEWTEST LTD",
        "roAddress.line1" -> "Testing Bld",
        "roAddress.line2" -> "1 Test Street",
        "roAddress.line3" -> "Testshire",
        "roAddress.line4" -> "Testford",
        "roAddress.postCode" -> "TE1 1ST",
        "roAddress.country" -> "UK",
        "ppobAddress.line1" -> "Testing Bld",
        "ppobAddress.line2" -> "1 Test Street",
        "ppobAddress.line3" -> "Testshire",
        "ppobAddress.line4" -> "Testford",
        "ppobAddress.postCode" -> "TE1 1ST",
        "ppobAddress.country" -> "UK",
        "businessContactDetails.businessEmail" -> "test@test.com",
        "businessContactDetails.mobileNumber" -> "1234567",
        "businessContactDetails.phoneNumber" -> "099876545"
      )

      val model = CompanyDetails(
        crn = Some("abc"),
        companyName = "TEST LTD",
        tradingName = Some("NEWTEST LTD"),
        roAddress = Address(
          line1 = "Testing Bld",
          line2 = "1 Test Street",
          line3 = Some("Testshire"),
          line4 = Some("Testford"),
          postCode = Some("TE1 1ST"),
          country = Some("UK")
        ),
        ppobAddress = Address(
          line1 = "Testing Bld",
          line2 = "1 Test Street",
          line3 = Some("Testshire"),
          line4 = Some("Testford"),
          postCode = Some("TE1 1ST"),
          country = Some("UK")
        ),
        businessContactDetails = DigitalContactDetails(
          email = Some("test@test.com"),
          mobileNumber = Some("1234567"),
          phoneNumber = Some("099876545")
        )
      )

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
          "crn" -> "abc",
          "companyName" -> "TEST LTD",
          "tradingName" -> "NEWTEST LTD",
          "roAddress.line1" -> "Testing Bld",
          "roAddress.line2" -> "1 Test Street",
          "roAddress.line3" -> "Testshire",
          "roAddress.line4" -> "Testford",
          "roAddress.postCode" -> "TE1 1ST",
          "roAddress.country" -> "UK",
          "ppobAddress.line1" -> "Testing Bld",
          "ppobAddress.line2" -> "1 Test Street",
          "ppobAddress.line3" -> "Testshire",
          "ppobAddress.line4" -> "Testford",
          "ppobAddress.postCode" -> "TE1 1ST",
          "ppobAddress.country" -> "UK",
          "businessContactDetails.businessEmail" -> "test@test.com",
          "businessContactDetails.mobileNumber" -> "1234567",
          "businessContactDetails.phoneNumber" -> "099876545"
        )
      }
    }

    "Bind successfully with not full data" should {
      val data = Map(
        "crn" -> "",
        "companyName" -> "TEST LTD",
        "tradingName" -> "",
        "roAddress.line1" -> "Testing Bld",
        "roAddress.line2" -> "1 Test Street",
        "roAddress.line3" -> "",
        "roAddress.line4" -> "",
        "roAddress.postCode" -> "TE1 1ST",
        "roAddress.country" -> "UK",
        "ppobAddress.line1" -> "Testing Bld",
        "ppobAddress.line2" -> "1 Test Street",
        "ppobAddress.line3" -> "",
        "ppobAddress.line4" -> "Testford",
        "ppobAddress.postCode" -> "TE1 1ST",
        "ppobAddress.country" -> "UK",
        "businessContactDetails.businessEmail" -> "test@test.com",
        "businessContactDetails.mobileNumber" -> "",
        "businessContactDetails.phoneNumber" -> "099876545"
      )

      val model = CompanyDetails(
        crn = None,
        companyName = "TEST LTD",
        tradingName = None,
        roAddress = Address(
          line1 = "Testing Bld",
          line2 = "1 Test Street",
          line3 = None,
          line4 = None,
          postCode = Some("TE1 1ST"),
          country = Some("UK")
        ),
        ppobAddress = Address(
          line1 = "Testing Bld",
          line2 = "1 Test Street",
          line3 = None,
          line4 = Some("Testford"),
          postCode = Some("TE1 1ST"),
          country = Some("UK")
        ),
        businessContactDetails = DigitalContactDetails(
          email = Some("test@test.com"),
          mobileNumber = None,
          phoneNumber = Some("099876545")
        )
      )

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
          "companyName" -> "TEST LTD",
          "roAddress.line1" -> "Testing Bld",
          "roAddress.line2" -> "1 Test Street",
          "roAddress.postCode" -> "TE1 1ST",
          "roAddress.country" -> "UK",
          "ppobAddress.line1" -> "Testing Bld",
          "ppobAddress.line2" -> "1 Test Street",
          "ppobAddress.line4" -> "Testford",
          "ppobAddress.postCode" -> "TE1 1ST",
          "ppobAddress.country" -> "UK",
          "businessContactDetails.businessEmail" -> "test@test.com",
          "businessContactDetails.phoneNumber" -> "099876545"
        )
      }
    }
  }

  "Have the correct error" when {
    "company name is not completed" in {
      val data: Map[String, String] = Map(
        "roAddress.line1" -> "Testing Bld",
        "roAddress.line2" -> "1 Test Street",
        "roAddress.postCode" -> "TE1 1ST",
        "roAddress.country" -> "UK",
        "ppobAddress.line1" -> "Testing Bld",
        "ppobAddress.line2" -> "1 Test Street",
        "ppobAddress.line4" -> "Testford",
        "ppobAddress.postCode" -> "TE1 1ST",
        "ppobAddress.country" -> "UK",
        "businessContactDetails.businessEmail" -> "test@test.com",
        "businessContactDetails.phoneNumber" -> "099876545"
      )
      val boundForm = testForm.bind(data)
      val nameError = FormError("companyName", "error.required")

      boundForm.errors shouldBe Seq(nameError)
      boundForm.data shouldBe data
    }

    "roAddress line1 and line2 are not completed" in {
      val data: Map[String, String] = Map(
        "companyName" -> "TEST LTD",
        "roAddress.postCode" -> "TE1 1ST",
        "roAddress.country" -> "UK",
        "ppobAddress.line1" -> "Testing Bld",
        "ppobAddress.line2" -> "1 Test Street",
        "ppobAddress.line4" -> "Testford",
        "ppobAddress.postCode" -> "TE1 1ST",
        "ppobAddress.country" -> "UK",
        "businessContactDetails.businessEmail" -> "test@test.com",
        "businessContactDetails.phoneNumber" -> "099876545"
      )
      val boundForm = testForm.bind(data)
      val formErrorRoAddressLine1 = FormError("roAddress.line1", "error.required")
      val formErrorRoAddressLine2 = FormError("roAddress.line2", "error.required")

      boundForm.error("roAddress.line1") shouldBe Some(formErrorRoAddressLine1)
      boundForm.error("roAddress.line2") shouldBe Some(formErrorRoAddressLine2)
      boundForm.data shouldBe data
    }

    "ppobAddress line1 and line2 are not completed" in {
      val data: Map[String, String] = Map(
        "companyName" -> "TEST LTD",
        "roAddress.line1" -> "Testing Bld",
        "roAddress.line2" -> "1 Test Street",
        "roAddress.postCode" -> "TE1 1ST",
        "roAddress.country" -> "UK",
        "ppobAddress.line4" -> "Testford",
        "ppobAddress.postCode" -> "TE1 1ST",
        "ppobAddress.country" -> "UK",
        "businessContactDetails.businessEmail" -> "test@test.com",
        "businessContactDetails.phoneNumber" -> "099876545"
      )
      val boundForm = testForm.bind(data)
      val formErrorPPOBAddressLine1 = FormError("ppobAddress.line1", "error.required")
      val formErrorPPOBAddressLine2 = FormError("ppobAddress.line2", "error.required")

      boundForm.error("ppobAddress.line1") shouldBe Some(formErrorPPOBAddressLine1)
      boundForm.error("ppobAddress.line2") shouldBe Some(formErrorPPOBAddressLine2)
      boundForm.data shouldBe data
    }
  }
}
