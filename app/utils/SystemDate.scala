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

package utils

import java.time.LocalDateTime

import org.joda.time.DateTime
import org.joda.time.chrono.ISOChronology
import uk.gov.hmrc.time.CurrentTaxYear

object SystemDate extends SystemDateT

  trait SystemDateT extends CurrentTaxYear {
  def getSystemDate: LocalDateTime = Option(System.getProperty("feature.system-date")).fold(LocalDateTime.now()) {
    case "" => LocalDateTime.now()
    case date => LocalDateTime.parse(date)
  }

  override def now: () => DateTime = { () =>
    val getSystem = getSystemDate
    new DateTime(
      getSystem.getYear,
      getSystem.getMonthValue,
      getSystem.getDayOfMonth,
      getSystem.getHour,
      getSystem.getMinute,
      getSystem.getSecond,
      getSystem.getNano / 1000000,
      ISOChronology.getInstanceUTC)
  }
}
