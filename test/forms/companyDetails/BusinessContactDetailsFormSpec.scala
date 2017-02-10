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

import models.BusinessContactDetails
import play.api.data.{Form, FormError, Mapping}
import uk.gov.hmrc.play.test.UnitSpec

class BusinessContactDetailsFormSpec extends UnitSpec {

  val testForm = BusinessContactDetailsForm.form

  "Binding BusinessContactDetailsForm to a model" should {
    "Bind successfully with full data" in {
      val data = Map(
        "businessEmail" -> "testEmail@testing.com",
        "mobileNumber" -> "01234567987",
        "phoneNumber" -> "07798123456"
      )
      val model = BusinessContactDetails(
        businessEmail = Some("testEmail@testing.com"),
        mobileNumber = Some("01234567987"),
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with minimal data (email)" in {
      val data = Map(
        "businessEmail" -> "testEmail@testing.com"
      )
      val model = BusinessContactDetails(
        businessEmail = Some("testEmail@testing.com"),
        mobileNumber = None,
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with minimal data (mobile)" in {
      val data = Map(
        "mobileNumber" -> "01234567987"
      )
      val model = BusinessContactDetails(
        businessEmail = None,
        mobileNumber = Some("01234567987"),
        phoneNumber = None
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Bind successfully with minimal data (phone)" in {
      val data = Map(
        "phoneNumber" -> "07798123456"
      )
      val model = BusinessContactDetails(
        businessEmail = None,
        mobileNumber = None,
        phoneNumber = Some("07798123456")
      )

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Have the correct error if no fields are completed" in {
      val data: Map[String,String] = Map()
      val boundForm = testForm.bind(data)
      val formError = FormError("noFieldsCompleted-businessEmail", BusinessContactDetailsForm.noFieldsCompletedMessage)

      boundForm shouldBe testForm.withError(formError)
    }

    "Have the correct error if email is invalid" in {
      val data: Map[String,String] = Map(
        "businessEmail" -> "NotAProperEmail!!!!!"
      )
      val boundForm = testForm.bind(data)
      val errForm = Form(
        testForm.mapping,
        Map("businessEmail" -> "NotAProperEmail!!!!!"),
        List(FormError("businessEmail",List("errors.invalid.email"),List())),
        None
      )

      boundForm shouldBe errForm
    }

    "Have the correct error if mobile number is invalid" in {
      val data: Map[String,String] = Map(
        "mobileNumber" -> "NotAProperMobNumber!!!!!"
      )
      val boundForm = testForm.bind(data)
      val errForm = Form(
        testForm.mapping,
        Map("mobileNumber" -> "NotAProperMobNumber!!!!!"),
        List(FormError("mobileNumber",List("errors.invalid.mobileNumber"),List())),
        None
      )

      boundForm shouldBe errForm
    }

    "Have the correct error if phone number is invalid" in {
      val data: Map[String,String] = Map(
        "phoneNumber" -> "NotAProperNumber!!!!!"
      )
      val boundForm = testForm.bind(data)

      val errForm = Form(
        testForm.mapping,
        Map("phoneNumber" -> "NotAProperNumber!!!!!"),
        List(FormError("phoneNumber",List("errors.invalid.phoneNumber"),List())),
        None
      )

      boundForm shouldBe errForm
    }
  }

}
