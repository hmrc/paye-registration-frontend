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
import uk.gov.hmrc.play.test.UnitSpec

class DateUtilSpec extends UnitSpec with DateUtil {
  "calling toDate" should {
    "return a date" in {
      toDate("2016", "12", "31") shouldBe LocalDate.parse("2016-12-31")
    }
  }

  "calling fromDate" should {
    "return a tuple3 (String, String, String)" in {
      fromDate(LocalDate.parse("2016-12-31")) shouldBe (("2016", "12", "31"))
    }
  }

  "calling lessOrEqualThanXDaysAfter" should {
    "return false if the date is more than 61 days in the future (case leap year)" in {
      lessOrEqualThanXDaysAfter(LocalDate.parse("2016-02-29"), LocalDate.parse("2016-05-01"), 61) shouldBe false
    }
  }

  "calling lessOrEqualThanXDaysAfter" should {
    "return false if the date is more than 61 days in the future" in {
      lessOrEqualThanXDaysAfter(LocalDate.parse("2017-02-28"), LocalDate.parse("2017-05-01"), 61) shouldBe false
    }
  }

  "calling lessOrEqualThanXDaysAfter" should {
    "return true if the date is less than 61 days in the future" in {
      lessOrEqualThanXDaysAfter(LocalDate.parse("2016-12-31"), LocalDate.parse("2017-03-01"), 61) shouldBe true
    }
  }

  "calling lessOrEqualThanXDaysAfter" should {
    "return true if the date is equal to 61 days in the future" in {
      lessOrEqualThanXDaysAfter(LocalDate.parse("2016-12-31"), LocalDate.parse("2017-03-02"), 61) shouldBe true
    }
  }
}
