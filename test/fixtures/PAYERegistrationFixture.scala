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
import models.api._
import models.view.{Address, Summary, SummaryRow, SummarySection}

trait PAYERegistrationFixture {

  val validCompanyDetailsAPI = CompanyDetails(
    crn = None,
    companyName = "Test Company",
    tradingName = Some("Test Company Trading Name"),
    Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
    businessContactDetails = validBusinessContactDetails
  )

  val validDate = LocalDate.of(2016,12,20)
  val validEmploymentAPI = Employment(employees = true,
                                  companyPension = Some(true),
                                  subcontractors = true,
                                  firstPayDate = validDate)

  val validBusinessContactDetails =
    BusinessContactDetails(
      Some("test@email.com"),
      Some("1234567890"),
      Some("0987654321")
    )

  val validPAYERegistrationAPI = PAYERegistration(
    registrationID = "AC123456",
    formCreationTimestamp = "2017-01-11T15:10:12",
    companyDetails = validCompanyDetailsAPI,
    employment = validEmploymentAPI,
    directors = Nil
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

  val validEmploymentAPIModel = Employment(
    true,
    Some(true),
    true,
    LocalDate.of(2016, 12, 20)
  )

  val validROAddress = Address(
    line1 = "14 St Test Walker",
    line2 = "Testford",
    line3 = Some("Testley"),
    line4 = None,
    country = Some("UK"),
    postCode = Some("TE1 1ST")
  )

  def validName(f: String, m: Option[String], l:String) = Name(Some(f), m, Some(l), None)

  val validDirectorList = Seq(Director(validName("Bob", None, "Smith"), Some("NINO")), Director(validName("Michael", Some("Jay"), "Fudgedybar"), None))
}
