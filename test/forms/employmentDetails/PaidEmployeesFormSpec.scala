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
import forms.employmentDetails.PaidEmployeesForm.{dateTimeFormat, customFormPrefix}
import helpers.PayeComponentSpec
import models.view.EmployingAnyone
import play.api.data.FormError

class PaidEmployeesFormSpec extends PayeComponentSpec {

  val today = LocalDate.now

  val incorpDate2MonthsPrior = today.minusMonths(2)
  val incorpDate3YearsPrior = today.minusYears(3)
  val testFormIncorped2MonthsAgo = PaidEmployeesForm.form(incorpDate2MonthsPrior)
  val testFormIncorped3YearsAgo = PaidEmployeesForm.form(incorpDate3YearsPrior)

  val notPayingAnyone = Map("alreadyPaying" -> "false")

  val payingSomeoneAllowableDate = Map(
    "alreadyPaying" -> "true",
    "earliestDateDay" -> today.getDayOfMonth.toString,
    "earliestDateMonth" -> today.getMonthValue.toString,
    "earliestDateYear" -> today.getYear.toString
  )

  val payingSomeoneNoDate = Map(
    "alreadyPaying" -> "true"
  )

  val payingSomeone3YearsAgo = Map(
    "alreadyPaying" -> "true",
    "earliestDateDay" -> today.getDayOfMonth.toString,
    "earliestDateMonth" -> today.getMonthValue.toString,
    "earliestDateYear" -> today.minusYears(3).getYear.toString
  )

  val payingSomeoneInvalidDate = Map(
    "alreadyPaying" -> "true",
    "earliestDateDay" -> "32",
    "earliestDateMonth" -> today.getMonthValue.toString,
    "earliestDateYear" -> today.minusYears(1).getYear.toString
  )

  val payingSomeone2YearsAgo = Map(
    "alreadyPaying" -> "true",
    "earliestDateDay" -> today.getDayOfMonth.toString,
    "earliestDateMonth" -> today.getMonthValue.toString,
    "earliestDateYear" -> today.minusYears(2).getYear.toString
  )

  val payingSomeoneInFuture = Map(
    "alreadyPaying" -> "true",
    "earliestDateDay" -> today.plusDays(1).getDayOfMonth.toString,
    "earliestDateMonth" -> today.getMonthValue.toString,
    "earliestDateYear" -> today.getYear.toString
  )

  "PaidEmployeesForm" should {
    "return a completed form when providing not providing payments" in {
      testFormIncorped2MonthsAgo.bind(notPayingAnyone).value.get mustBe EmployingAnyone(false, None)
    }
    "return an error when not passing in a date when making payments" in {
      testFormIncorped2MonthsAgo.bind(payingSomeoneNoDate).errors mustBe Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.empty"))
    }
    "return a completed form when paying someone and providing a date which is allowable" in {
      testFormIncorped2MonthsAgo.bind(payingSomeoneAllowableDate).value.get mustBe EmployingAnyone(true, Some(LocalDate.of(today.getYear, today.getMonthValue, today.getDayOfMonth)))
    }
    "return a form error when passing in a paying date before date of incorp" in {
      testFormIncorped2MonthsAgo.bind(payingSomeone2YearsAgo).errors mustBe Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.dateTooEarly", Seq(incorpDate2MonthsPrior.format(dateTimeFormat))))
    }
    "return a form error when passing in a paying date on or after incorp date, but more than 2 years in the past" in {
      testFormIncorped3YearsAgo.bind(payingSomeone3YearsAgo).errors mustBe Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.moreThanTwoYears"))
    }
    "return a form error when passing in an invalid date" in {
      testFormIncorped3YearsAgo.bind(payingSomeoneInvalidDate).errors mustBe Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.invalid"))
    }
    "return a form error when passing in a future date" in {
      testFormIncorped3YearsAgo.bind(payingSomeoneInFuture).errors mustBe Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.dateInFuture"))
    }
  }

}
