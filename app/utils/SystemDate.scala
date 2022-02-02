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

package utils

import uk.gov.hmrc.time.CurrentTaxYear

import java.time.{LocalDate, LocalDateTime}

object SystemDate extends SystemDateT

trait SystemDateT extends CurrentTaxYear {
  def getSystemDate: LocalDateTime = Option(System.getProperty("feature.system-date")).map {
    case "" => LocalDateTime.now()
    case date => LocalDateTime.parse(date)
  }.getOrElse(LocalDateTime.now())

  override def now: () => LocalDate = { () =>
   getSystemDate.toLocalDate
  }
}
