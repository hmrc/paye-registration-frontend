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

package utils

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import forms.completionCapacity.CompletionCapacityForm
import forms.employmentDetails.FirstPaymentForm
import uk.gov.hmrc.play.test.UnitSpec

class ValidatorsSpec extends UnitSpec with DateUtil {
  "calling isInvalidDate" should {
    "return an error message if the day is not valid" in {
      val data : Map[String, String] = Map(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "32")
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) shouldBe List("app.common.date.invalid")
    }

    "return an error message if the month is not valid" in {
      val data : Map[String, String] = Map(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "13",
        "firstPayDay" -> "31")
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) shouldBe List("app.common.date.invalid")
    }

    "return an error message if the year is not valid" in {
      val data : Map[String, String] = Map(
        "firstPayYear" -> "-3",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "31")
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) shouldBe List("app.common.date.invalid")
    }
  }

  "calling firstPaymentDateRange" should {
    "return an error message if the date is more than 61 days in the future" in {
      val today = LocalDate.now()
      val futureDate = fromDate(today.plus(3, ChronoUnit.MONTHS))
      val data : Map[String, String] = Map(
        "firstPayYear" -> futureDate._1,
        "firstPayMonth" -> futureDate._2,
        "firstPayDay" -> futureDate._3)
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) shouldBe List("pages.firstPayment.date.invalidRange")
    }
  }

  "calling nonEmpty" should {
    "return an error message if the field is empty" in {
      val data : Map[String, String] = Map(
        "completionCapacity" -> "other",
        "completionCapacityOther" -> "")
      val boundForm = CompletionCapacityForm.form.bind(data)
      boundForm.errors.map(_.message) shouldBe List("errors.invalid.emptyField")
    }
    "pass if the field is completed" in {
      val data : Map[String, String] = Map(
        "completionCapacity" -> "other",
        "completionCapacityOther" -> "summat")
      val boundForm = CompletionCapacityForm.form.bind(data)
      boundForm.errors.map(_.message) shouldBe List.empty
    }
  }
}
