/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json.Json

case class EmployingStaff(employingAnyone: Option[EmployingAnyone],
                          willBePaying: Option[WillBePaying],
                          construction: Option[Boolean],
                          subcontractors: Option[Boolean],
                          companyPension: Option[Boolean])

case class EmployingAnyone(employing: Boolean,
                           startDate: Option[LocalDate])

case class WillBePaying(willPay: Boolean,
                        beforeSixApril: Option[Boolean])

object EmployingStaff {
  implicit val formatEmployingAnyone  = Json.format[EmployingAnyone]
  implicit val formatWillbePaying     = Json.format[WillBePaying]
  implicit val format                 = Json.format[EmployingStaff]
}
