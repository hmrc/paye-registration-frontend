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

package services

import config.AppConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class ThresholdService @Inject()(appConfig: AppConfig){

  def now: LocalDate = LocalDate.now()

  def getCurrentThresholds: Map[String, Int] = {
    val taxYearStartDate = LocalDate.parse(appConfig.thresholdSwitchDate)

    val isTaxYear = now.isEqual(taxYearStartDate) || now.isAfter(taxYearStartDate)

    val weeklyThreshold = appConfig.currentPayeWeeklyThreshold
    val monthlyThreshold = appConfig.currentPayeMonthlyThreshold
    val annualThreshold = appConfig.currentPayeAnnualThreshold

    val oldWeeklyThreshold = appConfig.oldPayeWeeklyThreshold
    val oldMonthlyThreshold = appConfig.oldPayeMonthlyThreshold
    val oldAnnualThreshold = appConfig.oldPayeAnnualThreshold

    if (isTaxYear) {
      Map("weekly" -> weeklyThreshold, "monthly" -> monthlyThreshold, "annually" -> annualThreshold)
    }
    else {
      Map("weekly" -> oldWeeklyThreshold, "monthly" -> oldMonthlyThreshold, "annually" -> oldAnnualThreshold)
    }
  }

}
