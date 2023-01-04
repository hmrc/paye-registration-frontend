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

package forms.helpers

import helpers.PayeComponentSpec
import play.api.data.FormError

class OneOfManyFormSpec extends PayeComponentSpec {

  object TestForm extends OneOfManyForm {
    override val optionalFields = Seq("test1", "test2", "test3")
    override val noFieldsCompletedMessage = "noFieldsErrMsg"
  }

  "Binding a form extending OneOfManyForm" should {
    "bind from one field completed" in {
      val data = Map[String, String](
        "test1" -> "test",
        "test2" -> ""
      )
      TestForm.multiPartFormatter.bind("test1", data) mustBe Right(Some("test"))
      TestForm.multiPartFormatter.bind("test2", data) mustBe Right(None)
      TestForm.multiPartFormatter.bind("test3", data) mustBe Right(None)
    }

    "bind with another field completed" in {
      val data = Map[String, String](
        "test2" -> "test"
      )
      TestForm.multiPartFormatter.bind("test1", data) mustBe Right(None)
      TestForm.multiPartFormatter.bind("test2", data) mustBe Right(Some("test"))
      TestForm.multiPartFormatter.bind("test3", data) mustBe Right(None)
    }

    "fail to bind with no fields completed" in {
      val data = Map[String, String](
      )
      TestForm.multiPartFormatter.bind("test1", data) mustBe Left(Seq(FormError("test1", "noFieldsErrMsg")))
    }

    "fail to bind with all fields blank" in {
      val data = Map[String, String](
        "test1" -> "",
        "test2" -> "",
        "test3" -> ""
      )
      TestForm.multiPartFormatter.bind("test1", data) mustBe Left(Seq(FormError("test1", "noFieldsErrMsg")))
    }

    "fail to bind with other, non included fields completed" in {
      val data = Map[String, String](
        "unIncludedField" -> "string",
        "nextUnIncludedField" -> "string"
      )
      TestForm.multiPartFormatter.bind("test1", data) mustBe Left(Seq(FormError("test1", "noFieldsErrMsg")))
    }

    "bind with multiple fields completed" in {
      val data = Map[String, String](
        "test1" -> "string",
        "test2" -> "otherString"
      )
      TestForm.multiPartFormatter.bind("test1", data) mustBe Right(Some("string"))
      TestForm.multiPartFormatter.bind("test2", data) mustBe Right(Some("otherString"))
      TestForm.multiPartFormatter.bind("test3", data) mustBe Right(None)
    }
  }

  "Unbinding a form extending OneOfManyForm" should {
    "Unbind successfully from Some" in {
      TestForm.multiPartFormatter.unbind("tst", Some("test value")) mustBe Map("tst" -> "test value")
    }

    "Unbind successfully from None" in {
      TestForm.multiPartFormatter.unbind("tst", None) mustBe Map("tst" -> "")
    }
  }

}
