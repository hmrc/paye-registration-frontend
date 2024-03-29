/*
 * Copyright 2023 HM Revenue & Customs
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

package helpers.fixtures

import enums.PAYEStatus
import models.api.{PAYEContact => PAYEContactAPI, _}
import models.view.{EmployingAnyone, EmployingStaff, PAYEContactDetails, Summary, SummaryChangeLink, SummaryRow, SummarySection, WillBePaying, PAYEContact => PAYEContactView}
import models.{Address, DigitalContactDetails}

import java.time.LocalDate

trait PAYERegistrationFixture {

  val validBusinessContactDetails =
    DigitalContactDetails(
      Some("test@email.com"),
      Some("1234567890"),
      Some("0987654321")
    )

  val validCompanyDetailsAPI = CompanyDetails(
    companyName = "Test Company",
    tradingName = Some("Test Company Trading Name"),
    Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
    Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK")),
    businessContactDetails = validBusinessContactDetails
  )

  val validDate = LocalDate.of(2016, 12, 20)

  val validEmploymentApi = Employment(
    employees = Employing.willEmployNextYear,
    firstPaymentDate = LocalDate.of(2018, 1, 1),
    construction = true,
    subcontractors = true,
    companyPension = None
  )
  val validEmploymentApiIncorporated = Employment(
    employees = Employing.alreadyEmploying,
    firstPaymentDate = LocalDate.of(2016, 12, 20),
    construction = true,
    subcontractors = true,
    companyPension = Some(true)
  )

  val validEmploymentApiNotIncorporated = Employment(
    employees = Employing.willEmployThisYear,
    firstPaymentDate = LocalDate.of(2016, 12, 20),
    construction = true,
    subcontractors = true,
    companyPension = None
  )

  val validEmploymentViewIncorporated = EmployingStaff(
    Some(EmployingAnyone(true, Some(LocalDate.of(2016, 12, 20)))),
    None,
    Some(true),
    Some(true),
    Some(true)
  )

  val validEmploymentViewNotIncorporated = EmployingStaff(
    None,
    Some(WillBePaying(true, None)),
    Some(true),
    Some(true),
    None
  )


  val validSICCodes = List(SICCode(None, Some("Accounting")))

  val validPAYEContactAPI = PAYEContactAPI(
    PAYEContactDetails(
      name = "testName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("testEmail"),
        mobileNumber = Some("1234567890"),
        phoneNumber = Some("0987654321")
      )
    ),
    Address("22 Test test", "Testerarium", None, None, Some("TE0 0ST"))
  )

  val validPAYEContactView = PAYEContactView(
    contactDetails = Some(PAYEContactDetails(
      name = "testName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("testEmail"),
        mobileNumber = Some("1234567890"),
        phoneNumber = Some("0987654321")
      )
    )),
    correspondenceAddress = Some(Address("22 Test test", "Testerarium", None, None, Some("TE0 0ST")))
  )

  val emptyPAYEContactView = PAYEContactView(None, None)

  val validPAYERegistrationAPI = PAYERegistration(
    registrationID = "AC123456",
    transactionID = "10-1028374",
    formCreationTimestamp = "2017-01-11T15:10:12",
    status = PAYEStatus.draft,
    completionCapacity = "High Priest",
    companyDetails = validCompanyDetailsAPI,
    employmentInfo = validEmploymentApi,
    sicCodes = validSICCodes,
    directors = Nil,
    payeContact = validPAYEContactAPI
  )

  lazy val validSummaryView = Summary(
    Seq(SummarySection(
      id = "tradingName",
      sectionHeading = "testSectionHeading",
      rows = Seq(SummaryRow(
        id = "tradingName",
        question = "testQuestion",
        answers = Seq("tstTrade"),
        optChangeLink = Some(SummaryChangeLink(
          controllers.userJourney.routes.CompanyDetailsController.tradingName,
          "testTradingNameHiddenText"
        ))
      ))
    ))
  )


  val validROAddress = Address(
    line1 = "14 St Test Walker",
    line2 = "Testford",
    line3 = Some("Testley"),
    line4 = None,
    country = Some("UK"),
    postCode = Some("TE1 1ST")
  )

  def validName(f: String, m: Option[String], l: String) = Name(Some(f), m, Some(l), None)

  val validDirectorList = Seq(Director(validName("Bob", None, "Smith"), Some("NINO")), Director(validName("Michael", Some("Jay"), "Fudgedybar"), None))

  val validSICCodesList = Seq(
    SICCode(
      code = None,
      description = Some("laundring")
    ),
    SICCode(
      code = Some("1234"),
      description = Some("consulting")
    )
  )

  val validPAYEContactDetails = PAYEContactDetails(
    name = "Thierry Henry",
    digitalContactDetails = DigitalContactDetails(
      Some("speedy@gonzalez.com"),
      Some("9999"),
      Some("0986534")
    )
  )
}