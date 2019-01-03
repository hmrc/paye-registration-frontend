/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate
import javax.inject.Inject

import utils.SystemDate

class ThresholdServiceImpl @Inject()() extends ThresholdService {
  override def now: LocalDate   = SystemDate.getSystemDate.toLocalDate
  override val nextTaxYearStart = LocalDate.of(2018, 4, 6)
}

trait ThresholdService {
  protected def now: LocalDate

  protected def nextTaxYearStart: LocalDate

  //TODO: Raise another story to make this more robust
  def getCurrentThresholds: Map[String, Int] = {
    if(now.isEqual(nextTaxYearStart) | now.isAfter(nextTaxYearStart)) {
      buildThresholdMap(116, 503, 6032)
    } else {
      buildThresholdMap(113, 490, 5876)
    }
  }

  private def buildThresholdMap(weekly: Int, monthly: Int, annually: Int): Map[String, Int] = {
    Map("weekly" -> weekly, "monthly" -> monthly, "annually" -> annually)
  }
}
