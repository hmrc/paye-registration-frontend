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

package forms.employmentDetails

import helpers.PayeComponentSpec
import play.api.data.FormError

class PaysPensionFormSpec extends PayeComponentSpec {

  val testForm = PaysPensionForm.form
  val paysPension = Map("paysPension" -> "true")
  val doesNotPayPension = Map("paysPension" -> "false")
  val noEntry = Map("paysPension" -> "")

  "PaysPensionForm" should {
    "return true if user makes pension payments" in {
      testForm.bind(paysPension).value.get mustBe true
    }
    "return false if user doesn't make pension payments" in {
      testForm.bind(doesNotPayPension).value.get mustBe false
    }
    "return an error if the user makes no selection" in {
      testForm.bind(noEntry).errors mustBe Seq(FormError("paysPension", "pages.paysPension.error", Nil))
    }

  }
}
