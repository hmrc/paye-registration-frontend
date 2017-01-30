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

package fixtures

import java.time.LocalDate

import models.view._

trait S4LFixture {

  val validTradingNameViewModel = TradingName(
    differentName = true,
    tradingName = Some("Test Company Trading Name")
  )

  val negativeTradingNameViewModel = TradingName(
    differentName = false,
    tradingName = None
  )

  val validCompanyDetailsViewModel = CompanyDetails(
    Some("crn"),
    "Tst Company Name",
    Some(validTradingNameViewModel)
  )

  val validEmploymentViewModel = Employment(
    Some(EmployingStaff(true)),
    Some(CompanyPension(true)),
    Some(Subcontractors(true)),
    Some(FirstPayment(LocalDate.of(2016,12,20)))
  )

  val incompleteEmploymentViewModel = Employment(
    Some(EmployingStaff(true)),
    None,
    None,
    None
  )
}
