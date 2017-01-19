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

import models.api.{PAYERegistration, CompanyDetails}
import models.view.{SummaryRow, SummarySection, Summary}

trait PAYERegistrationFixture {

  val validCompanyDetailsAPI = CompanyDetails(
    crn = None,
    companyName = "Test Company",
    tradingName = Some("Test Company Trading Name")
  )


  val validPAYERegistrationAPI = PAYERegistration(
    registrationID = "AC123456",
    formCreationTimestamp = "2017-01-11T15:10:12",
    companyDetails = validCompanyDetailsAPI
  )

  lazy val validSummaryView = Summary(
    Seq(SummarySection(
      id="tradingName",
      Seq(SummaryRow(
        id="tradingName",
        answer = Right("tstTrade"),
        changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
      ))
    ))
  )

}
