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

package utils

import java.time.temporal.ChronoUnit

import forms.employmentDetails.FirstPaymentForm
import helpers.PayeComponentSpec

class ValidatorsSpec extends PayeComponentSpec with DateUtil {
  "calling isInvalidDate" should {
    "return an error message if the day is not valid" in {
      val data : Map[String, String] = Map(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "32")
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) mustBe List("pages.firstPayment.date.invalidRange")
    }

    "return an error message if the month is not valid" in {
      val data : Map[String, String] = Map(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "13",
        "firstPayDay" -> "31")
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) mustBe List("pages.firstPayment.date.invalidRange")
    }

    "return an error message if the year is not valid" in {
      val data : Map[String, String] = Map(
        "firstPayYear" -> "-3",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "31")
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) mustBe List("pages.firstPayment.date.invalidRange")
    }
  }

  "calling firstPaymentDateRange" should {
    "return an error message if the date is more than 61 days in the future" in {
      val today = SystemDate.getSystemDate.toLocalDate
      val futureDate = fromDate(today.plus(3, ChronoUnit.MONTHS))
      val data : Map[String, String] = Map(
        "firstPayYear" -> futureDate._1,
        "firstPayMonth" -> futureDate._2,
        "firstPayDay" -> futureDate._3)
      val boundForm = FirstPaymentForm.form.bind(data)
      boundForm.errors.map(_.message) mustBe List("pages.firstPayment.date.invalidRange")
    }
  }

  "dateRegex" should {
    "validate 2018-01-01T12:00:00" in {
      assert("2018-01-01T12:00:00".matches(Validators.datePatternRegex))
    }

    "not validate 2018-1-1" in {
      assert(!"2018-1-1".matches(Validators.datePatternRegex))
    }
  }

  "desSchemaRegex" should {
    "not validate FAKE_SOD::TR9873!^^7FDFNN" in {
      assert(!"FAKE_SOD::TR9873!^^7FDFNN".matches(Validators.desSessionRegex))
    }
    "validate stubbed-1sds-sdijhi-2383-seei" in {
      assert("stubbed-1sds-sdijhi-2383-seei".matches(Validators.desSessionRegex))
    }
  }
}
