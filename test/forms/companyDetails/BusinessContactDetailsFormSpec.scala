/*
 * Copyright 2020 HM Revenue & Customs
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
        "businessEmail" -> "testEmail@testing.aaaaaaaaaaa",
        "mobileNumber" -> "01234567987",
        "phoneNumber" -> "07798123456"
      )
      val model = DigitalContactDetails(
        email = Some("testEmail@testing.aaaaaaaaaaa"),
        mobileNumber = Some("01234567987"),
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).get
      boundModel mustBe model
    }

    "Bind successfully with full data with email at full length" in {
      val data = Map(
        "businessEmail" -> "test@emailllllllllllllllllllllllllllllllllllllllllllllllll.aaaaaaaaaaa",
        "mobileNumber" -> "01234567987",
        "phoneNumber" -> "07798123456"
      )
      val model = DigitalContactDetails(
        email = Some("test@emailllllllllllllllllllllllllllllllllllllllllllllllll.aaaaaaaaaaa"),
        mobileNumber = Some("01234567987"),
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).get
      boundModel mustBe model
    }

    "Bind successfully with minimal data (email 2.11 )" in {
      val data = Map(
        "businessEmail" -> "testEmail@co.aaaaaaaaaaa",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val model = DigitalContactDetails(
        email = Some("testEmail@co.aaaaaaaaaaa"),
        mobileNumber = None,
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).get
      boundModel mustBe model
    }

    "Bind successfully with minimal data (email 11.11)" in {
      val data = Map(
        "businessEmail" -> "testEmail@aaaaaaaaaaa.aaaaaaaaaaa",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val model = DigitalContactDetails(
        email = Some("testEmail@aaaaaaaaaaa.aaaaaaaaaaa"),
        mobileNumber = None,
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).get
      boundModel mustBe model
    }

    "Bind successfully with minimal data (email 2.11.5)" in {
      val data = Map(
        "businessEmail" -> "testEmail@co.aaaaaaaaaaa.bbbbb",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val model = DigitalContactDetails(
        email = Some("testEmail@co.aaaaaaaaaaa.bbbbb"),
        mobileNumber = None,
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).get
      boundModel mustBe model
    }

    "Have the correct error if length after final full stop is too long" in {
      val data: Map[String, String] = Map(
        "businessEmail" -> "testEmail@test.cccccccccccc",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "testEmail@test.cccccccccccc",
          "mobileNumber" -> "",
          "phoneNumber" -> ""
        ),
        List(FormError("businessEmail", List("errors.invalid.email"), List())),
        None
      )

      boundForm mustBe errForm
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

      val boundModel = testForm.bind(data).get
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

      val boundModel = testForm.bind(data).get
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
        "businessEmail" -> "test-email-address@test-email-address-that-is-just-far-far-too-long-to-be-valid.aaaaaaaaaaa",
        "mobileNumber" -> "",
        "phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)

      val errForm = Form(
        testForm.mapping,
        Map(
          "businessEmail" -> "test-email-address@test-email-address-that-is-just-far-far-too-long-to-be-valid.aaaaaaaaaaa",
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
