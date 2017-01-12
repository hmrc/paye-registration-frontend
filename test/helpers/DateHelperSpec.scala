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

package helpers

import java.time.LocalDateTime

import uk.gov.hmrc.play.test.UnitSpec

class DateHelperSpec extends UnitSpec {

  val tstDateHelper = DateHelper

  "formatTimeStamp" should {
    "Correctly format a LocalDateTime to a String" in {
      // date time of 12:35 on 20th Feb, 2017
      val tstDate = LocalDateTime.of(2017, 2, 20, 12, 35, 0)

      tstDateHelper.formatTimestamp(tstDate) shouldBe "2017-02-20T12:35:00"
    }

    "Correctly format a LocalDateTime with nanoseconds to a String" in {
      // date time of 12:35.3 on 20th Feb, 2017
      val tstDate = LocalDateTime.of(2017, 2, 20, 12, 35, 0, 300000000)

      tstDateHelper.formatTimestamp(tstDate) shouldBe "2017-02-20T12:35:00"
    }
  }
}
