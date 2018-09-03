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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import helpers.PayeComponentSpec

class DateUtilSpec extends PayeComponentSpec with DateUtil {
  "calling toDate" should {
    "return a date" in {
      toDate("2016", "03", "01") mustBe LocalDate.parse("2016-03-01")
    }
  }

  "calling toDate with a day set as 1 and month set as 3" should {
    "return a date" in {
      toDate("2016", "3", "1") mustBe LocalDate.parse("2016-03-01")
    }
  }

  "calling toDate with a day set as 30 and month set as 2 and year as 2017" should {
    "return an exception" in {
      an[Exception] mustBe thrownBy(toDate("2017", "2", "30"))
    }
  }

  "calling fromDate with a date format yyyy-MM-dd" should {
    "return a tuple3 (year: String, month: String, day: String)" in {
      fromDate(LocalDate.parse("2016-12-31")) mustBe (("2016", "12", "31"))
    }
  }

  "calling fromDate with a date format dd-MM-yyyy" should {
    "return a tuple3 (year: String, month: String, day: String)" in {
      fromDate(LocalDate.parse("31-12-2016", DateTimeFormatter.ofPattern("dd-MM-yyyy"))) mustBe (("2016", "12", "31"))
    }
  }
}
