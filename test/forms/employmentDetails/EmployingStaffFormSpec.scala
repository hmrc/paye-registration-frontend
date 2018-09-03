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

import java.time.LocalDate

import helpers.PayeComponentSpec
import models.view.WillBePaying
import play.api.data.FormError

class EmployingStaffFormSpec extends PayeComponentSpec {

  val payInNext2MonthsBeforeNTY = Map("willBePaying" -> "true", "beforeNewTaxYear" -> "true")
  val payInNext2MonthsAfterNTY = Map("willBePaying" -> "true", "beforeNewTaxYear" -> "false")
  val notPayingInNext2Months = Map("willBePaying" -> "false")

  val doesNotPayPension = Map("paysPension" -> "false")

  val noEntry = Map("willBePaying" -> "", "beforeNewTaxYear" -> "")
  val noEntrySecondAnswer = Map("willBePaying" -> "true", "beforeNewTaxYear" -> "")

  "EmployingStaffForm" should {
    val now = LocalDate.of(2017, 2, 6)
    val testForm = EmployingStaffForm.form(now)

    "return completed form if user will pay in next 2 months before new tax year" in {
      testForm.bind(payInNext2MonthsBeforeNTY).value.get mustBe WillBePaying(true, Some(true))
    }
    "return completed form if user will pay in next 2 months after new tax year" in {
      testForm.bind(payInNext2MonthsAfterNTY).value.get mustBe WillBePaying(true, Some(false))
    }
    "return completed form if user will not pay in next 2 months" in {
      testForm.bind(notPayingInNext2Months).value.get mustBe WillBePaying(false, None)
    }
    "return error if first answer not provided" in {
      testForm.bind(noEntry).errors mustBe Seq(FormError("willBePaying", "pages.willBePaying.empty", Nil))
    }
    "return error if first answer provided but second not provided" in {
      testForm.bind(noEntrySecondAnswer).errors mustBe Seq(FormError("beforeNewTaxYear", "pages.willBePaying.beforeNewTaxYear.empty", Nil))
    }
    "return completed if only first answer provided, current date not between 6th Feb and 5th Apr" in {
      val onlyFirstAnswer = Map("willBePaying" -> "true")
      val now2 = LocalDate.of(2017, 2, 5)
      val testForm2 = EmployingStaffForm.form(now2)
      testForm2.bind(onlyFirstAnswer).value.get mustBe WillBePaying(true, None)
    }
  }
}
