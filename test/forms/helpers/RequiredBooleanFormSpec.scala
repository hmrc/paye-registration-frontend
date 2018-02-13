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

package forms.helpers

import helpers.PayeComponentSpec
import play.api.data.FormError

class RequiredBooleanFormSpec extends PayeComponentSpec {

  object TestForm extends RequiredBooleanForm

  "A YesNoForm" should {
    "bind from true" in {
      TestForm.requiredBooleanFormatter.bind("tstKey", Map[String, String]("tstKey" -> "true")) mustBe Right(true)
    }

    "bind from false" in {
      TestForm.requiredBooleanFormatter.bind("tstKey", Map[String, String]("tstKey" -> "false")) mustBe Right(false)
    }

    "fail to bind from \"\"" in {
      TestForm.requiredBooleanFormatter.bind("tstKey", Map[String, String]("tstKey" -> "")) mustBe Left(Seq(FormError("tstKey", "error.required", Nil)))
    }

    "unbind from true" in {
      TestForm.requiredBooleanFormatter.unbind("tstKey", true) mustBe Map("tstKey" -> "true")
    }

    "unbind from false" in {
      TestForm.requiredBooleanFormatter.unbind("tstKey", false) mustBe Map("tstKey" -> "false")
    }
  }
}
