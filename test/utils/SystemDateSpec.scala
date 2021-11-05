/*
 * Copyright 2021 HM Revenue & Customs
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

import helpers.PayeComponentSpec

import java.time.{LocalDate, LocalDateTime}

class SystemDateSpec extends PayeComponentSpec {

  override protected def beforeEach(): Unit = {
    System.clearProperty("feature.system-date")
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    System.clearProperty("feature.system-date")
    super.afterEach()
  }

  "getSystemDate" should {
    "return a LocalDate of today" when {
      "the feature is null" in {
        val result = SystemDate.getSystemDate
        result.toLocalDate mustBe LocalDate.now
      }

      "the feature is ''" in {
        System.setProperty("feature.system-date", "")

        val result = SystemDate.getSystemDate
        result.toLocalDate mustBe LocalDate.now
      }
    }

    "return a LocalDate that was previously set" in {
      System.setProperty("feature.system-date", "2018-01-01T12:00:00")

      val result = SystemDate.getSystemDate
      result mustBe LocalDateTime.parse("2018-01-01T12:00:00")
    }
  }
}
