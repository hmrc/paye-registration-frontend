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

import common.exceptions.InternalExceptions.APIConversionException
import connectors.PAYERegistrationConnector
import fixtures.PAYERegistrationFixture
import models.BusinessContactDetails
import models.api.{CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI, SICCode, Director, Employment, Name}
import models.view.{SummaryRow, SummarySection, Summary, Address}
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{NotFoundException, Upstream4xxResponse, HeaderCarrier}

import scala.concurrent.Future

class SummaryServiceSpec  extends PAYERegSpec with PAYERegistrationFixture {

  val mockRegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new SummaryService (mockKeystoreConnector, mockRegConnector)
  }

  val simpleSICCodes = List(SICCode(code = None, description = Some("Firearms")))

  val apiRegistration = PAYERegistrationAPI(
    registrationID = "AC123456",
    formCreationTimestamp = "2017-01-11T15:10:12",
    companyDetails = CompanyDetailsAPI(
      crn = None,
      companyName = "Test Company",
      tradingName = Some("tstTrade"),
      Address("14 St Test Walk", "Testley", None, None, None, None),
      Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK")),
      businessContactDetails = validBusinessContactDetails
    ),
    employment = validEmploymentAPI,
    sicCodes = simpleSICCodes,
    directors = List(
      Director(
        name = Name(
          forename = Some("Timothy"),
          otherForenames = Some("Potterley-Smythe"),
          surname = Some("Buttersford"),
          title = Some("Mr")
        ),
        nino = Some("ZZ123456A")
      )
    )
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
          ),
          SummaryRow(
            id = "ppobAddress",
            answer = Right("15 St Test Avenue<br />Testpool<br />TestUponAvon<br />Nowhereshire<br />LE1 1ST<br />UK"),
            Some(controllers.userJourney.routes.CompanyDetailsController.roAddress())
          ),
          SummaryRow(
            id = "natureOfBusiness",
            answer = Right("Firearms"),
            Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness())
          )
        )
      ),
      SummarySection(
        id = "businessContactDetails",
        Seq(
          SummaryRow(
            id = "businessEmail",
            answer = Right("test@email.com"),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          ),
          SummaryRow(
            id = "mobileNumber",
            answer = Right("1234567890"),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          ),
          SummaryRow(
            id = "businessTelephone",
            answer = Right("0987654321"),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
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
      ),
      SummarySection(
        id = "directorDetails",
        Seq(
          SummaryRow(
            id = "director0",
            answer = Right("ZZ123456A"),
            Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
            Some(Seq("Timothy Buttersford")),
            Some("director")
          )
        )
      )
    )
  )

  implicit val hc = HeaderCarrier()

  val forbidden = Upstream4xxResponse("403", 403, 403)
  val notFound = new NotFoundException("404")
  val runTimeException = new RuntimeException("tst")

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
          Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
          Address("15 St Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE4 1ST"), Some("UK")),
          businessContactDetails = validBusinessContactDetails
        ),
        employment = validEmploymentAPI,
        sicCodes = simpleSICCodes,
        directors = List(
          Director(
            name = Name(
              forename = Some("Timothy"),
              otherForenames = Some("Potterley-Smythe"),
              surname = Some("Buttersford"),
              title = Some("Mr")
            ),
            nino = Some("ZZ123456A")
          )
        )
      )

      val formatHMTLROAddress = service.formatHTMLROAddress(apiRegistrationNoTName.companyDetails.roAddress)
      val formatHMTLPPOBAddress = service.formatHTMLROAddress(apiRegistrationNoTName.companyDetails.ppobAddress)

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
              ),
              SummaryRow(
                id = "ppobAddress",
                answer = Right(formatHMTLPPOBAddress),
                Some(controllers.userJourney.routes.CompanyDetailsController.roAddress())
              ),
              SummaryRow(
                id = "natureOfBusiness",
                answer = Right("Firearms"),
                Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness())
              )
            )
          ),
          SummarySection(
            id = "businessContactDetails",
            Seq(
              SummaryRow(
                id = "businessEmail",
                answer = Right("test@email.com"),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
              ),
              SummaryRow(
                id = "mobileNumber",
                answer = Right("1234567890"),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
              ),
              SummaryRow(
                id = "businessTelephone",
                answer = Right("0987654321"),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
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
          ),
          SummarySection(
            id = "directorDetails",
            Seq(
              SummaryRow(
                id = "director0",
                answer = Right("ZZ123456A"),
                Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
                Some(Seq("Timothy Buttersford")),
                Some("director")
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
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE4 1ST"), Some("UK")),
        BusinessContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )

      val sicCodes = List(
        SICCode(
          code = None,
          description = Some("Novelty hairbrushes")
        )
      )

      val formatHMTLROAddress = service.formatHTMLROAddress(validCompanyDetailsAPI.roAddress)
      val formatHMTLPPOBAddress = service.formatHTMLROAddress(validCompanyDetailsAPI.ppobAddress)

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
          ),
          SummaryRow(
            id = "ppobAddress",
            answer = Right(formatHMTLPPOBAddress),
            Some(controllers.userJourney.routes.CompanyDetailsController.roAddress())
          ),
          SummaryRow(
            id = "natureOfBusiness",
            answer = Right("Novelty hairbrushes"),
            Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness())
          )
        )
      )

      service.buildCompanyDetailsSection(validCompanyDetailsAPI, sicCodes) shouldBe companyDetailsSection
    }

    "throw the correct exception when there is no description provided" in new Setup{
      val sicCodesModel = List(
        SICCode(
          code = None,
          description = None
        )
      )

      an[APIConversionException] shouldBe thrownBy(service.buildCompanyDetailsSection(validCompanyDetailsAPI, sicCodesModel))
    }

    "throw the correct exception when there is no SIC code provided" in new Setup{
      val sicCodesModel = List.empty

      a[NoSuchElementException] shouldBe thrownBy(service.buildCompanyDetailsSection(validCompanyDetailsAPI, sicCodesModel))
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
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              answer = Right("1234567890"),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              answer = Right("0987654321"),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
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
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              answer = Left("noAnswerGiven"),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              answer = Left("noAnswerGiven"),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            ))
          ).flatten
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) shouldBe validBCDSection
    }
  }

  "buildDirectorsSection" should {
    "return a valid director details block" in new Setup {
      val directorDetailsModel = List(
        Director(
          name = Name(
            forename = Some("Timothy"),
            otherForenames = Some("Potterley-Smythe"),
            surname = Some("Buttersford"),
            title = Some("Mr")
          ),
          nino = Some("ZZ123456A")
        ),
        Director(
          name = Name(
            forename = Some("Pierre"),
            otherForenames = Some("Paul"),
            surname = Some("Simpson"),
            title = Some("Mr")
          ),
          nino = None
        )
      )


      val validDirectorDetailsSection =
        SummarySection(
          id = "directorDetails",
          Seq(
            Some(SummaryRow(
              id = "director0",
              answer = Right("ZZ123456A"),
              changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
              questionArgs = Some(Seq("Timothy Buttersford")),
              commonQuestionKey = Some("director")
            )),
            Some(SummaryRow(
              id = "director1",
              answer = Right(""),
              changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
              questionArgs = Some(Seq("Pierre Simpson")),
              commonQuestionKey = Some("director")
            ))
          ).flatten
        )

      service.buildDirectorsSection(directorDetailsModel) shouldBe validDirectorDetailsSection
    }
  }
}
