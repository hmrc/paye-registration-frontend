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

import models.BusinessContactDetails
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
    Some(validTradingNameViewModel),
    Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
    Some(BusinessContactDetails(Some("test@paye.co.uk"), None, None))
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

  val validBusinessContactModel = BusinessContactDetails(
    businessEmail = Some("test@paye.co.uk"),
    mobileNumber = Some("00447123456789"),
    phoneNumber = Some("0044123456789")
  )
}
