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

package forms.employmentDetails

import forms.employmentDetails.PaidEmployeesForm.{customFormPrefix, dateTimeFormat}
import helpers.PayeComponentSpec
import models.view.EmployingAnyone
import play.api.data.FormError
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PaidEmployeesFormSpec extends PayeComponentSpec {

  class Setup(ourDate: LocalDate = LocalDate.now()) {
    val testForm = new PaidEmployeesFormT {
      override val now = ourDate
    }
  }

  val today = LocalDate.now
  val cty = TaxYear.taxYearFor(LocalDate.now)

  val incorpDate2MonthsPrior = today.minusMonths(2)
  val incorpDateWellInPast = LocalDate.of(2015, 1, 1)

  val notPayingAnyone = Map("alreadyPaying" -> "false")

  val payingSomeoneAllowableDate = Map(
    "alreadyPaying" -> "true",
    "earliestDate.Day" -> today.getDayOfMonth.toString,
    "earliestDate.Month" -> today.getMonthValue.toString,
    "earliestDate.Year" -> today.getYear.toString
  )

  val payingSomeoneNoDate = Map(
    "alreadyPaying" -> "true"
  )

  val payingSomeoneMoreThan2TaxYearsAgo = Map(
    "alreadyPaying" -> "true",
    "earliestDate.Day" -> "5",
    "earliestDate.Month" -> "4",
    "earliestDate.Year" -> today.minusYears(5).getYear.toString
  )

  val payingSomeoneExactlyTwoTaxYearsAgo = Map(
    "alreadyPaying" -> "true",
    "earliestDate.Day" -> "6",
    "earliestDate.Month" -> "4",
    "earliestDate.Year" -> today.minusYears(2).getYear.toString
  )

  val payingSomeoneInvalidDate = Map(
    "alreadyPaying" -> "true",
    "earliestDate.Day" -> "32",
    "earliestDate.Month" -> today.getMonthValue.toString,
    "earliestDate.Year" -> today.minusYears(1).getYear.toString
  )

  val payingSomeone2YearsAgo = Map(
    "alreadyPaying" -> "true",
    "earliestDate.Day" -> today.getDayOfMonth.toString,
    "earliestDate.Month" -> today.getMonthValue.toString,
    "earliestDate.Year" -> today.minusYears(2).getYear.toString
  )
  val futureDate = today.plus(1, ChronoUnit.DAYS)
  val payingSomeoneInFuture = Map(
    "alreadyPaying" -> "true",
    "earliestDate.Day" -> futureDate.getDayOfMonth.toString,
    "earliestDate.Month" -> futureDate.getMonthValue.toString,
    "earliestDate.Year" -> futureDate.getYear.toString
  )

  "PaidEmployeesForm" should {
    "return a completed form when providing not providing payments" in new Setup(LocalDate.now) {
      testForm.form(incorpDate2MonthsPrior).bind(notPayingAnyone).value.get mustBe EmployingAnyone(false, None)
    }
    "return an error when not passing in a date when making payments" in new Setup(LocalDate.now) {
      testForm.form(incorpDate2MonthsPrior).bind(payingSomeoneNoDate).errors mustBe Seq(FormError(s"${customFormPrefix}", "pages.paidEmployees.date.empty" , Seq(s"${customFormPrefix}.Day")))
    }
    "return a completed form when paying someone and providing a date which is allowable" in new Setup(LocalDate.now) {
      testForm.form(incorpDate2MonthsPrior).bind(payingSomeoneAllowableDate).value.get mustBe EmployingAnyone(true, Some(LocalDate.of(today.getYear, today.getMonthValue, today.getDayOfMonth)))
    }
    "return a form error when passing in a paying date before date of incorp" in new Setup(LocalDate.now) {
      testForm.form(incorpDate2MonthsPrior).bind(payingSomeone2YearsAgo).errors mustBe Seq(FormError(s"${customFormPrefix}", "pages.paidEmployees.date.dateTooEarly", Seq(s"${customFormPrefix}.Day", incorpDate2MonthsPrior.format(dateTimeFormat))))
    }
    "return a form error when passing in a paying date on or after incorp date, but more than 2 tax years in the past" in new Setup(LocalDate.of(2018, 6, 19)) {
      testForm.form(incorpDateWellInPast).bind(payingSomeoneMoreThan2TaxYearsAgo).errors mustBe Seq(FormError(s"${customFormPrefix}", "pages.paidEmployees.date.moreThanTwoTaxYears", Seq(s"${customFormPrefix}.Day")))
    }
    "return a form error when passing in an invalid date" in new Setup(LocalDate.now) {
       testForm.form(incorpDateWellInPast).bind(payingSomeoneInvalidDate).errors mustBe Seq(FormError(s"${customFormPrefix}.Day", "pages.paidEmployees.date.invalid", Seq(s"${customFormPrefix}.Day")))
    }
    "return a form error when passing in a future date" in new Setup(LocalDate.now) {
      testForm.form(incorpDateWellInPast).bind(payingSomeoneInFuture).errors mustBe Seq(FormError(s"${customFormPrefix}", "pages.paidEmployees.date.dateInFuture", Seq(s"${customFormPrefix}.Day")))
    }

    "return a completed form when paying someone and providing a date which is exactly two tax years ago" in new Setup(LocalDate.of(today.getYear, 6, 19)) {
      testForm.form(incorpDateWellInPast).bind(payingSomeoneExactlyTwoTaxYearsAgo).value.get mustBe EmployingAnyone(true, Some(LocalDate.of(today.minusYears(2).getYear, 4, 6)))
    }
    "return a completed form when paying someone on the last day of the current tax year and providing a date which is exactly two tax years ago" in new Setup(LocalDate.of(today.getYear, 4, 5)) {
      testForm.form(incorpDateWellInPast).bind(payingSomeoneExactlyTwoTaxYearsAgo).value.get mustBe EmployingAnyone(true, Some(LocalDate.of(today.minusYears(2).getYear, 4, 6)))
    }
    "return a completed form when paying someone on the first day of the current tax year and providing a date which is exactly two tax years ago" in new Setup(LocalDate.of(today.getYear, 4, 6)) {
      testForm.form(incorpDateWellInPast).bind(payingSomeoneExactlyTwoTaxYearsAgo).value.get mustBe EmployingAnyone(true, Some(LocalDate.of(today.minusYears(2).getYear, 4, 6)))
    }
  }
}