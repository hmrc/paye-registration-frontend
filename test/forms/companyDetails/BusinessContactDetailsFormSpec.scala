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

package forms.companyDetails

import helpers.PayeComponentSpec
import models.DigitalContactDetails
import play.api.data.{Form, FormError}

class BusinessContactDetailsFormSpec extends PayeComponentSpec {

  val testForm = BusinessContactDetailsForm.form

  "Binding BusinessContactDetailsForm to a model" should {
    "Bind successfully with full data" in {
      val data = Map(
        "businessEmail" -> "testEmail@testing.com",
        "mobileNumber" -> "01234567987",
        "phoneNumber" -> "07798123456"
      )
      val model = DigitalContactDetails(
        email = Some("testEmail@testing.com"),
        mobileNumber = Some("01234567987"),
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel mustBe model
    }

    "Bind successfully with full data with email at full length" in {
      val data = Map(
        "businessEmail" -> "test@emailllllllllllllllllllllllllllllllllllllllllllllllllllllllll.com",
        "mobileNumber" -> "01234567987",
        "phoneNumber" -> "07798123456"
      )
      val model = DigitalContactDetails(
        email = Some("test@emailllllllllllllllllllllllllllllllllllllllllllllllllllllllll.com"),
        mobileNumber = Some("01234567987"),
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel mustBe model
    }

    "Bind successfully with minimal data (email)" in {
      val data = Map(
        "businessEmail" -> "testEmail@testing.com",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val model = DigitalContactDetails(
        email = Some("testEmail@testing.com"),
        mobileNumber = None,
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel mustBe model
    }

    "Bind successfully with minimal data (mobile)" in {
      val data = Map(
        "businessEmail" -> "",
        "mobileNumber" -> "01234567987",
        "phoneNumber" -> ""
      )
      val model = DigitalContactDetails(
        email = None,
        mobileNumber = Some("01234567987"),
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel mustBe model
    }

    "Bind successfully with minimal data (phone)" in {
      val data = Map(
        "businessEmail" -> "",
        "mobileNumber" -> "",
        "phoneNumber" -> "07798123456"
      )
      val model = DigitalContactDetails(
        email = None,
        mobileNumber = None,
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel mustBe model
    }

    "Have the correct error if no fields are completed" in {
      val data: Map[String,String] = Map()
      val boundForm = testForm.bind(data)
      val formError = FormError("noFieldsCompleted-businessEmail", BusinessContactDetailsForm.noFieldsCompletedMessage)

      boundForm mustBe testForm.withError(formError)
    }

    "Have the correct error if email is invalid" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "NotAProperEmail",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "NotAProperEmail",
          "mobileNumber" -> "",
          "phoneNumber" -> ""
        ),
        List(FormError("businessEmail",List("errors.invalid.email"),List())),
        None
      )

      boundForm mustBe errForm
    }

    "Have the correct error if mobile number is invalid" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "",
        "mobileNumber" -> "NotAProperMobNumber!!!!!",
        "phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "",
          "mobileNumber" -> "NotAProperMobNumber!!!!!",
          "phoneNumber" -> ""
        ),
        List(FormError("mobileNumber",List("errors.invalid.contactNum"),List())),
        None
      )

      boundForm mustBe errForm
    }

    "Have the correct error if phone number is invalid" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "",
        "mobileNumber" -> "",
        "phoneNumber" -> "NotAProperNumber!!!!!"
      )
      val boundForm = testForm.bind(data)

      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "",
          "mobileNumber" -> "",
          "phoneNumber" -> "NotAProperNumber!!!!!"
        ),
        List(FormError("phoneNumber",List("errors.invalid.contactNum"),List())),
        None
      )

      boundForm mustBe errForm
    }

    "Have the correct error if the email is of a valid structure but too long" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "test-email-address@test-email-address-that-is-just-far-far-too-long-to-be-valid.com",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)

      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "test-email-address@test-email-address-that-is-just-far-far-too-long-to-be-valid.com",
          "mobileNumber" -> "",
          "phoneNumber" -> ""
        ),
        List(FormError("businessEmail",List("errors.invalid.email.tooLong"),List())),
        None
      )

      boundForm mustBe errForm
    }

    "Have the correct error if the mobile number is just a space" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "test@email.com",
        "mobileNumber" -> " ",
        "phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)

      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "test@email.com",
          "mobileNumber" -> " ",
          "phoneNumber" -> ""
        ),
        List(FormError("mobileNumber",List("errors.invalid.contactNum"),List())),
        None
      )

      boundForm mustBe errForm
    }

    "Have the correct error if the phone number is just a space" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "test@email.com",
        "mobileNumber" -> "",
        "phoneNumber" -> " "
      )
      val boundForm = testForm.bind(data)

      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "test@email.com",
          "mobileNumber" -> "",
          "phoneNumber" -> " "
        ),
        List(FormError("phoneNumber",List("errors.invalid.contactNum"),List())),
        None
      )

      boundForm mustBe errForm
    }
  }
}
