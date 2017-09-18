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

import play.api.data.{Form, FormError}
import play.api.data.Forms._
import uk.gov.hmrc.play.test.UnitSpec

class PhoneNoFormSpec extends UnitSpec {
  object TestForm extends PhoneNoForm

  "Binding a form extending PhoneNoForm" should {
    "bind from an empty input" in {
      val data = Map[String, String](
        "test1" -> ""
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Right(None)
    }

    "bind from a correct input with 10 digits" in {
      val data = Map[String, String](
        "test1" -> "0123456789"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Right(Some("0123456789"))
    }

    "bind from a correct input with 20 digits" in {
      val data = Map[String, String](
        "test1" -> "01234567890123456789"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Right(Some("01234567890123456789"))
    }

    "bind from a correct input with 15 digits and total less than 20 characters" in {
      val data = Map[String, String](
        "test1" -> "01 23 45 67 890 1234"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Right(Some("01 23 45 67 890 1234"))
    }

    "bind from a correct input with 15 digits and total more than 20 characters" in {
      val data = Map[String, String](
        "test1" -> "01 23 45 67 890 12 34"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Right(Some("012345678901234"))
    }

    "fail to bind with incorrect input containing non numeric characters" in {
      val data = Map[String, String](
        "test1" -> "a2324sfsf*&$%"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Left(Seq(FormError("test1", "errors.invalid.contactNum")))
    }

    "fail to bind with incorrect input, less than 10 digits" in {
      val data = Map[String, String](
        "test1" -> "012345678"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Left(Seq(FormError("test1", "errors.invalid.contactNum.tooShort")))
    }

    "fail to bind with incorrect input, more than 20 digits" in {
      val data = Map[String, String](
        "test1" -> "012345678901234567890"
      )
      TestForm.phoneNoFormatter.bind("test1", data) shouldBe Left(Seq(FormError("test1", "errors.invalid.contactNum.tooLong")))
    }
  }

  "Unbinding a form extending PhoneNoForm" should {
    "unbind successfully from Some" in {
      TestForm.phoneNoFormatter.unbind("tst", Some("test value")) shouldBe Map("tst" -> "test value")
    }

    "unbind successfully from None" in {
      TestForm.phoneNoFormatter.unbind("tst", None) shouldBe Map("tst" -> "")
    }
  }
}
