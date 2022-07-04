/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import config.AppConfig
import helpers.PayeComponentSpec
import play.api.Configuration
import utils.PAYEFeatureSwitch

import java.time.LocalDate

class ThresholdServiceSpec extends PayeComponentSpec {

  object TestAppConfig extends AppConfig(mock[Configuration], mock[PAYEFeatureSwitch]) {
    override lazy val taxYearStartDate: String = LocalDate.now().toString
    override lazy val currentPayeWeeklyThreshold: Int = 10
    override lazy val currentPayeMonthlyThreshold: Int = 20
    override lazy val currentPayeAnnualThreshold: Int = 30
    override lazy val oldPayeWeeklyThreshold: Int = 5
    override lazy val oldPayeMonthlyThreshold: Int = 10
    override lazy val oldPayeAnnualThreshold: Int = 15
  }

  "getCurrentThresholds" should {
    "return the old tax years thresholds if the date is before the tax year start date" in {
      object TestService extends ThresholdService(TestAppConfig) {
        override def now: LocalDate = LocalDate.now().minusDays(1)
      }

      val result = TestService.getCurrentThresholds
      result mustBe Map("weekly" -> 5, "monthly" -> 10, "annually" -> 15)
    }

    "return the new tax years thresholds if the date is on the tax year start date" in {
      object TestService extends ThresholdService(TestAppConfig)

      val result = TestService.getCurrentThresholds
      result mustBe Map("weekly" -> 10, "monthly" -> 20, "annually" -> 30)
    }

    "return the new tax years thresholds if the date is after the tax year start date" in {
      object TestService extends ThresholdService(TestAppConfig) {
        override def now: LocalDate = LocalDate.now().plusDays(1)
      }

      val result = TestService.getCurrentThresholds
      result mustBe Map("weekly" -> 10, "monthly" -> 20, "annually" -> 30)
    }
  }
}
