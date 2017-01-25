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

package models.view

import play.api.libs.json.Json


case class EmployingStaff(currentYear: Boolean)
object EmployingStaff {
  implicit val format = Json.format[EmployingStaff]
}

case class CompanyPension(pensionProvided: Boolean)
object CompanyPension {
  implicit val format = Json.format[CompanyPension]
}

case class Subcontractors(hasContractors: Boolean)
object Subcontractors {
  implicit val format = Json.format[Subcontractors]
}

case class FirstPayment(
                      firstPayYear: String,
                      firstPayMonth: String,
                      firstPayDay: String
                       )
object FirstPayment {
  implicit val format = Json.format[FirstPayment]
}

case class Employment(
                       employing: Option[EmployingStaff],
                       companyPension: Option[CompanyPension],
                       subcontractors: Option[Subcontractors],
                       firstPayment: Option[FirstPayment]
                     )
object Employment {
  implicit val format = Json.format[Employment]
}


