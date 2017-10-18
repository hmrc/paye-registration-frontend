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

package forms.payeContactDetails

import models.DigitalContactDetails
import models.view.PAYEContactDetails
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class PAYEContactDetailsFormSpec extends UnitSpec {

  val testForm = PAYEContactDetailsForm.form

  "Binding PAYEContactDetailsForm to a model" should {
    "Bind successfully with full data" in {
      val data = Map(
        "name" -> "test Mary-Jane84 de l'aurore",
        "digitalContact.contactEmail" -> "testEmail@testing.com",
        "digitalContact.mobileNumber" -> "01234567987",
        "digitalContact.phoneNumber" -> "07798123456"
      )
      val model = PAYEContactDetails(
        name = "test Mary-Jane84 de l'aurore",
        DigitalContactDetails(
          email = Some("testEmail@testing.com"),
          mobileNumber = Some("01234567987"),
          phoneNumber = Some("07798123456")
        )
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with full data with email at full length" in {
      val data = Map(
        "name" -> "test Mary-Jane84 de l'aurore",
        "digitalContact.contactEmail" -> "test@emailllllllllllllllllllllllllllllllllllllllllllllllllllllllll.com",
        "digitalContact.mobileNumber" -> "01234567987",
        "digitalContact.phoneNumber" -> "07798123456"
      )
      val model = PAYEContactDetails(
        name = "test Mary-Jane84 de l'aurore",
        DigitalContactDetails(
          email = Some("test@emailllllllllllllllllllllllllllllllllllllllllllllllllllllllll.com"),
          mobileNumber = Some("01234567987"),
          phoneNumber = Some("07798123456")
        )
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with minimal data (email)" in {
      val data = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "testEmail@testing.com",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val model = PAYEContactDetails(
        name = "testName",
        DigitalContactDetails(
          email = Some("testEmail@testing.com"),
          mobileNumber = None,
          phoneNumber = None
        )
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with minimal data (mobile)" in {
      val data = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "",
        "digitalContact.mobileNumber" -> "01234567987",
        "digitalContact.phoneNumber" -> ""
      )
      val model = PAYEContactDetails(
        name = "testName",
        DigitalContactDetails(
          email = None,
          mobileNumber = Some("01234567987"),
          phoneNumber = None
        )
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with minimal data (phone)" in {
      val data = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> "07798123456"
      )
      val model = PAYEContactDetails(
        name = "testName",
        DigitalContactDetails(
          email = None,
          mobileNumber = None,
          phoneNumber = Some("07798123456")
        )
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Have the correct error if no fields are completed" in {
      val data: Map[String,String] = Map(
        "name" -> "",
        "digitalContact.contactEmail" -> "",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val formError = FormError("noFieldsCompleted-digitalContact.contactEmail", PAYEContactDetailsForm.noFieldsCompletedMessage)
      val nameError = FormError("name", "pages.payeContact.nameMandatory")

      boundForm.error("name") shouldBe Some(nameError)
      boundForm.error("noFieldsCompleted-digitalContact.contactEmail") shouldBe Some(formError)
    }

    "Have the correct error if name is not completed" in {
      val data: Map[String,String] = Map(
        "name" -> "    ",
        "digitalContact.contactEmail" -> "email@business.net",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val nameError = FormError("name", "pages.payeContact.nameMandatory")


      boundForm.errors shouldBe Seq(nameError)
    }

    "Have the correct error if name is invalid" in {
      val data: Map[String,String] = Map(
        "name" -> "<h1>dgdgfd",
        "digitalContact.contactEmail" -> "email@business.net",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val nameError = FormError("name", "errors.invalid.name.invalidChars")


      boundForm.errors shouldBe Seq(nameError)
    }

    "Have the correct error if name is more than 100 characters" in {
      val data: Map[String,String] = Map(
        "name" -> "test Mary-Jane84 de l'aurore TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST",
        "digitalContact.contactEmail" -> "email@business.net",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val nameError = FormError("name", "errors.invalid.name.invalidChars")


      boundForm.errors shouldBe Seq(nameError)
    }

    "Have the correct error if email is invalid" in {
      val data: Map[String,String] = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "NotAProperEmail",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val err = FormError("digitalContact.contactEmail", "errors.invalid.email")


      boundForm.errors shouldBe Seq(err)
    }

    "Have the correct error if email is longer than 70 characters" in {
      val data: Map[String,String] = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "test@emaillllllllllllllllllllllllllllllllllllllllllllllllllllllllll.com",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val err = FormError("digitalContact.contactEmail", "errors.invalid.email.tooLong")


      boundForm.errors shouldBe Seq(err)
    }

    "Have the correct error if mobile number is invalid" in {
      val data: Map[String,String] = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "",
        "digitalContact.mobileNumber" -> "NotAProperMobNumber!!!!!",
        "digitalContact.phoneNumber" -> ""
      )
      val boundForm = testForm.bind(data)
      val err = FormError("digitalContact.mobileNumber", "errors.invalid.contactNum")

      boundForm.errors shouldBe Seq(err)
    }

    "Have the correct error if phone number is invalid" in {
      val data: Map[String,String] = Map(
        "name" -> "testName",
        "digitalContact.contactEmail" -> "",
        "digitalContact.mobileNumber" -> "",
        "digitalContact.phoneNumber" -> "NotAProperNumber!!!!!"
      )
      val boundForm = testForm.bind(data)
      val err = FormError("digitalContact.phoneNumber", "errors.invalid.contactNum")

      boundForm.errors shouldBe Seq(err)
    }
  }

}
