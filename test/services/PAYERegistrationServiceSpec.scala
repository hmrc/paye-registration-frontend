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

package services

import connectors._
import enums.DownstreamOutcome
import models.api.{Employment, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import models.view.{Address, Summary, SummaryRow, SummarySection}
import fixtures.PAYERegistrationFixture
import models.BusinessContactDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

class PAYERegistrationServiceSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockRegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new PAYERegistrationService (mockKeystoreConnector, mockRegConnector, mockS4LService)
  }

  val apiRegistration = PAYERegistrationAPI(
    registrationID = "AC123456",
    formCreationTimestamp = "2017-01-11T15:10:12",
    companyDetails = CompanyDetailsAPI(
      crn = None,
      companyName = "Test Company",
      tradingName = Some("tstTrade"),
      Address("14 St Test Walk", "Testley", None, None, None, None)
    ),
    businessContactDetails = validBusinessContactDetails,
    employment = validEmploymentAPI
  )

  lazy val formatHMTLROAddress = ""

  lazy val summary = Summary(
    Seq(
      SummarySection(
        id = "companyDetails",
        Seq(
          SummaryRow(
            id = "tradingName",
            answer = Right("tstTrade"),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
          ),
          SummaryRow(
            id = "roAddress",
            answer = Right("14 St Test Walk<br />Testley"),
            None
          )
        )
      ),
      SummarySection(
        id = "businessContactDetails",
        Seq(
          SummaryRow(
            id = "businessEmail",
            answer = Right("test@email.com"),
            changeLink = None
          ),
          SummaryRow(
            id = "mobileNumber",
            answer = Right("1234567890"),
            changeLink = None
          ),
          SummaryRow(
            id = "businessTelephone",
            answer = Right("0987654321"),
            changeLink = None
          )
        )
      ),
      SummarySection(
        id = "employees",
        Seq(
          SummaryRow(
            id = "employees",
            answer = Left("true"),
            Some(controllers.userJourney.routes.EmploymentController.employingStaff())
          ),
          SummaryRow(
            id = "companyPension",
            answer = Left("true"),
            Some(controllers.userJourney.routes.EmploymentController.companyPension())
          ),
          SummaryRow(
            id = "subcontractors",
            answer = Left("true"),
            Some(controllers.userJourney.routes.EmploymentController.subcontractors())
          ),
          SummaryRow(
            id = "firstPaymentDate",
            Right("20/12/2016"),
            Some(controllers.userJourney.routes.EmploymentController.firstPayment())
          )
        )
      )
    )
  )

  implicit val hc = HeaderCarrier()

  val forbidden = Upstream4xxResponse("403", 403, 403)
  val notFound = new NotFoundException("404")
  val runTimeException = new RuntimeException("tst")

  "Calling createNewRegistration" should {
    "return a success response when the Registration is successfully created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      await(service.assertRegistrationFootprint()) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when the Registration can't be created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      await(service.assertRegistrationFootprint()) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getRegistrationSummary" should {
    "return a defined Summary when the connector returns a valid PAYE Registration response" in new Setup {

      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(apiRegistration))

      await(service.getRegistrationSummary()) shouldBe summary
    }

    "return None when the connector returns a Forbidden response" in new Setup {
      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(forbidden))

      intercept[Upstream4xxResponse](await(service.getRegistrationSummary()))
    }

    "return None when the connector returns a Not Found response" in new Setup {
      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(notFound))

      intercept[NotFoundException](await(service.getRegistrationSummary()))
    }

    "return None when the connector returns an exception response" in new Setup {
      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(runTimeException))

      intercept[RuntimeException](await(service.getRegistrationSummary()))
    }
  }

  "Calling registrationToSummary" should {
    "convert a PAYE Registration API Model  to a summary model with a trading name" in new Setup {

      service.registrationToSummary(apiRegistration) shouldBe summary
    }

    "convert a PAYE Registration API Model  to a summary model without a trading name" in new Setup {


      val apiRegistrationNoTName = PAYERegistrationAPI(
        registrationID = "AC123456",
        formCreationTimestamp = "2017-01-11T15:10:12",
        companyDetails = CompanyDetailsAPI(
          crn = None,
          companyName = "Test Company",
          tradingName = None,
          Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))
        ),
        businessContactDetails = validBusinessContactDetails,
        employment = validEmploymentAPI
      )

      val formatHMTLROAddress = service.formatHTMLROAddress(apiRegistrationNoTName.companyDetails.roAddress)

      lazy val summaryNoTName = Summary(
        Seq(
          SummarySection(
            id = "companyDetails",
            Seq(
              SummaryRow(
                id = "tradingName",
                answer = Left("noAnswerGiven"),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
              ),
              SummaryRow(
                id = "roAddress",
                answer = Right(formatHMTLROAddress),
                None
              )
            )
          ),
          SummarySection(
            id = "businessContactDetails",
            Seq(
              SummaryRow(
                id = "businessEmail",
                answer = Right("test@email.com"),
                changeLink = None
              ),
              SummaryRow(
                id = "mobileNumber",
                answer = Right("1234567890"),
                changeLink = None
              ),
              SummaryRow(
                id = "businessTelephone",
                answer = Right("0987654321"),
                changeLink = None
              )
            )
          ),
          SummarySection(
            id = "employees",
            Seq(
              SummaryRow(
                id = "employees",
                answer = Left("true"),
                Some(controllers.userJourney.routes.EmploymentController.employingStaff())
              ),
              SummaryRow(
                id = "companyPension",
                answer = Left("true"),
                Some(controllers.userJourney.routes.EmploymentController.companyPension())
              ),
              SummaryRow(
                id = "subcontractors",
                answer = Left("true"),
                Some(controllers.userJourney.routes.EmploymentController.subcontractors())
              ),
              SummaryRow(
                id = "firstPaymentDate",
                Right("20/12/2016"),
                Some(controllers.userJourney.routes.EmploymentController.firstPayment())
              )
            )
          )
        )
      )
      service.registrationToSummary(apiRegistrationNoTName) shouldBe summaryNoTName
    }
  }

  "buildCompanyDetailsSection" should {
    "return a valid summary section" in new Setup {

      val validCompanyDetailsAPI = CompanyDetailsAPI(
        crn = None,
        companyName = "Test Company",
        tradingName = Some("Test Company Trading Name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))
      )

      val formatHMTLROAddress = service.formatHTMLROAddress(validCompanyDetailsAPI.roAddress)

      val companyDetailsSection = SummarySection(
        id = "companyDetails",
        Seq(
          SummaryRow(
            id = "tradingName",
            answer = Right("Test Company Trading Name"),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
          ),
          SummaryRow(
            id = "roAddress",
            answer = Right(formatHMTLROAddress),
            None
          )
        )
      )

      service.buildCompanyDetailsSection(validCompanyDetailsAPI) shouldBe companyDetailsSection
    }
  }

  "buildEmploymentSection" should {
    "return a valid summary section" in new Setup {

      val validEmploymentAPIFalse = Employment(employees = false,
        companyPension = Some(false),
        subcontractors = false,
        firstPayDate = validDate
      )

      val employmentSection = SummarySection(
        id = "employees",
        Seq(
          SummaryRow(
            id = "employees",
            answer = Left("false"),
            Some(controllers.userJourney.routes.EmploymentController.employingStaff())
          ),
          SummaryRow(
            id = "companyPension",
            answer = Left("false"),
            Some(controllers.userJourney.routes.EmploymentController.companyPension())
          ),
          SummaryRow(
            id = "subcontractors",
            answer = Left("false"),
            Some(controllers.userJourney.routes.EmploymentController.subcontractors())
          ),
          SummaryRow(
            id = "firstPaymentDate",
            Right("20/12/2016"),
            Some(controllers.userJourney.routes.EmploymentController.firstPayment())
          )
        )
      )

      service.buildEmploymentSection(validEmploymentAPIFalse) shouldBe employmentSection
    }
  }

  "buildBusinessContactDetails" should {
    "return a valid buiness contact details block" in new Setup {
      val businessContactDetailsModel =
        BusinessContactDetails(
          Some("test@email.com"),
          Some("1234567890"),
          Some("0987654321")
        )

      val validBCDSection =
        SummarySection(
          id = "businessContactDetails",
          Seq(
            Some(SummaryRow(
              id = "businessEmail",
              answer = Right("test@email.com"),
              changeLink = None
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              answer = Right("1234567890"),
              changeLink = None
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              answer = Right("0987654321"),
              changeLink = None
            ))
          ).flatten
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) shouldBe validBCDSection
    }

    "return a summary section with no provided answers" in new Setup {
      val businessContactDetailsModel =
        BusinessContactDetails(
          None,
          None,
          None
        )

      val validBCDSection =
        SummarySection(
          id = "businessContactDetails",
          Seq(
            Some(SummaryRow(
              id = "businessEmail",
              answer = Left("noAnswerGiven"),
              changeLink = None
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              answer = Left("noAnswerGiven"),
              changeLink = None
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              answer = Left("noAnswerGiven"),
              changeLink = None
            ))
          ).flatten
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) shouldBe validBCDSection
    }
  }
}
