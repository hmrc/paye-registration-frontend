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

package models.api

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Reads, Writes, __}

import java.time.LocalDate

case class Employment(employees: Employing.Value,
                      firstPaymentDate: LocalDate,
                      construction: Boolean,
                      subcontractors: Boolean,
                      companyPension: Option[Boolean])

object Employment {
  implicit val format: Format[Employment] = (
    (__ \ "employees").format[Employing.Value](Employing.format) and
      (__ \ "firstPaymentDate").format[LocalDate] and
      (__ \ "construction").format[Boolean] and
      (__ \ "subcontractors").format[Boolean] and
      (__ \ "companyPension").formatNullable[Boolean]
    ) (Employment.apply, unlift(Employment.unapply))
}

object Employing extends Enumeration {
  val alreadyEmploying : Value = Value
  val notEmploying : Value = Value
  val willEmployThisYear : Value = Value
  val willEmployNextYear : Value = Value

  implicit val format: Format[Employing.Value] = Format(Reads.enumNameReads(Employing), Writes.enumNameWrites)
}

