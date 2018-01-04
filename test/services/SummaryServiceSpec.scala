/*
 * Copyright 2018 HM Revenue & Customs
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
import enums.PAYEStatus
import fixtures.PAYERegistrationFixture
import models.api.{PAYEContact => PAYEContactAPI}
import models.api.{Director, Employment, Name, SICCode, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import models.view.{PAYEContactDetails, Summary, SummaryRow, SummarySection}
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, NotFoundException, Upstream4xxResponse }

class SummaryServiceSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockRegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new SummarySrv {
      val keystoreConnector = mockKeystoreConnector
      val payeRegistrationConnector = mockRegConnector
    }
  }

  val simpleSICCodes = List(SICCode(code = None, description = Some("Firearms")))

  val apiRegistration = PAYERegistrationAPI(
    registrationID = "AC123456",
    transactionID = "10-1028374",
    formCreationTimestamp = "2017-01-11T15:10:12",
    status = PAYEStatus.draft,
    completionCapacity = "High Priest",
    companyDetails = CompanyDetailsAPI(
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
          surname = "Buttersford",
          title = Some("Mr")
        ),
        nino = Some("ZZ123456A")
      )
    ),
    payeContact = validPAYEContactAPI
  )

  lazy val summary = Summary(
    Seq(
      SummarySection(
        id = "employees",
        Seq(
          SummaryRow(
            id = "employees",
            answers = List(Left("true")),
            Some(controllers.userJourney.routes.EmploymentController.employingStaff())
          ),
          SummaryRow(
            id = "companyPension",
            answers = List(Left("true")),
            Some(controllers.userJourney.routes.EmploymentController.companyPension())
          ),
          SummaryRow(
            id = "subcontractors",
            answers = List(Left("true")),
            Some(controllers.userJourney.routes.EmploymentController.subcontractors())
          ),
          SummaryRow(
            id = "firstPaymentDate",
            answers = List(Right("20/12/2016")),
            Some(controllers.userJourney.routes.EmploymentController.firstPayment())
          )
        )
      ),
      SummarySection(
        id = "completionCapacity",
        Seq(
          SummaryRow(
            id ="completionCapacity",
            answers = List(Right("High Priest")),
            changeLink = Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
          )
        )
      ),
      SummarySection(
        id = "companyDetails",
        Seq(
          SummaryRow(
            id = "tradingName",
            answers = List(Right("tstTrade")),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
          ),
          SummaryRow(
            id = "roAddress",
            answers = List(Right("14 St Test Walk"), Right("Testley")),
            None
          ),
          SummaryRow(
            id = "ppobAddress",
            answers = List(Right("15 St Test Avenue"), Right("Testpool"), Right("TestUponAvon"), Right("Nowhereshire"), Right("LE1 1ST"), Right("UK")),
            Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress())
          ),
          SummaryRow(
            id = "natureOfBusiness",
            answers = List(Right("Firearms")),
            Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness())
          )
        )
      ),
      SummarySection(
        id = "businessContactDetails",
        Seq(
          SummaryRow(
            id = "businessEmail",
            answers = List(Right("test@email.com")),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          ),
          SummaryRow(
            id = "mobileNumber",
            answers = List(Right("1234567890")),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          ),
          SummaryRow(
            id = "businessTelephone",
            answers = List(Right("0987654321")),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          )
        )
      ),
      SummarySection(
        id = "directorDetails",
        Seq(
          SummaryRow(
            id = "director0",
            answers = List(Right("ZZ 12 34 56 A")),
            Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
            Some(Seq("Timothy Buttersford")),
            Some("director")
          )
        )
      ),
      SummarySection(
        id = "payeContactDetails",
        Seq(
          SummaryRow(
            id = "contactName",
            answers = List(Right("testName")),
            changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
          ),
          SummaryRow(
            id = "emailPAYEContact",
            answers = List(Right("testEmail")),
            changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
          ),
          SummaryRow(
            id = "mobileNumberPAYEContact",
            answers = List(Right("1234567890")),
            changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
          ),
          SummaryRow(
            id = "phoneNumberPAYEContact",
            answers = List(Right("0987654321")),
            changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
          ),
          SummaryRow(
            id = "correspondenceAddress",
            answers = List(Right("22 Test test"), Right("Testerarium"), Right("TE0 0ST")),
            changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress())
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

      when(mockRegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(apiRegistration))

      await(service.getRegistrationSummary("45632")) shouldBe summary
    }

    "return None when the connector returns a Forbidden response" in new Setup {
      when(mockRegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(forbidden))

      intercept[Upstream4xxResponse](await(service.getRegistrationSummary("45632")))
    }

    "return None when the connector returns a Not Found response" in new Setup {
      when(mockRegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(notFound))

      intercept[NotFoundException](await(service.getRegistrationSummary("45632")))
    }

    "return None when the connector returns an exception response" in new Setup {
      when(mockRegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(runTimeException))

      intercept[RuntimeException](await(service.getRegistrationSummary("45632")))
    }
  }

  "Calling registrationToSummary" should {
    "convert a PAYE Registration API Model  to a summary model with a trading name" in new Setup {

      service.registrationToSummary(apiRegistration) shouldBe summary
    }

    "convert a PAYE Registration API Model  to a summary model without a trading name" in new Setup {


      val apiRegistrationNoTName = PAYERegistrationAPI(
        registrationID = "AC123456",
        transactionID = "10-1028374",
        formCreationTimestamp = "2017-01-11T15:10:12",
        status = PAYEStatus.draft,
        completionCapacity = "High Priestess",
        companyDetails = CompanyDetailsAPI(
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
              surname = "Buttersford",
              title = Some("Mr")
            ),
            nino = Some("ZZ123456A")
          )
        ),
        payeContact = validPAYEContactAPI
      )

      val formatHMTLROAddress = service.addressToSummaryRowAnswers(apiRegistrationNoTName.companyDetails.roAddress)
      val formatHMTLPPOBAddress = service.addressToSummaryRowAnswers(apiRegistrationNoTName.companyDetails.ppobAddress)
      val formatHMTLCorrespondenceAddress = service.addressToSummaryRowAnswers(validPAYEContactAPI.correspondenceAddress)

      lazy val summaryNoTName = Summary(
        Seq(
          SummarySection(
            id = "employees",
            Seq(
              SummaryRow(
                id = "employees",
                answers = List(Left("true")),
                Some(controllers.userJourney.routes.EmploymentController.employingStaff())
              ),
              SummaryRow(
                id = "companyPension",
                answers = List(Left("true")),
                Some(controllers.userJourney.routes.EmploymentController.companyPension())
              ),
              SummaryRow(
                id = "subcontractors",
                answers = List(Left("true")),
                Some(controllers.userJourney.routes.EmploymentController.subcontractors())
              ),
              SummaryRow(
                id = "firstPaymentDate",
                answers = List(Right("20/12/2016")),
                Some(controllers.userJourney.routes.EmploymentController.firstPayment())
              )
            )
          ),
          SummarySection(
            id = "completionCapacity",
            Seq(
              SummaryRow(
                id ="completionCapacity",
                answers = List(Right("High Priestess")),
                changeLink = Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
              )
            )
          ),
          SummarySection(
            id = "companyDetails",
            Seq(
              SummaryRow(
                id = "tradingName",
                answers = List(Left("noAnswerGiven")),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
              ),
              SummaryRow(
                id = "roAddress",
                answers = formatHMTLROAddress,
                None
              ),
              SummaryRow(
                id = "ppobAddress",
                answers = formatHMTLPPOBAddress,
                Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress())
              ),
              SummaryRow(
                id = "natureOfBusiness",
                answers = List(Right("Firearms")),
                Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness())
              )
            )
          ),
          SummarySection(
            id = "businessContactDetails",
            Seq(
              SummaryRow(
                id = "businessEmail",
                answers = List(Right("test@email.com")),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
              ),
              SummaryRow(
                id = "mobileNumber",
                answers = List(Right("1234567890")),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
              ),
              SummaryRow(
                id = "businessTelephone",
                answers = List(Right("0987654321")),
                changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
              )
            )
          ),
          SummarySection(
            id = "directorDetails",
            Seq(
              SummaryRow(
                id = "director0",
                answers = List(Right("ZZ 12 34 56 A")),
                Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
                Some(Seq("Timothy Buttersford")),
                Some("director")
              )
            )
          ),
          SummarySection(
            id = "payeContactDetails",
            Seq(
              SummaryRow(
                id = "contactName",
                answers = List(Right("testName")),
                changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
              ),
              SummaryRow(
                id = "emailPAYEContact",
                answers = List(Right("testEmail")),
                changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
              ),
              SummaryRow(
                id = "mobileNumberPAYEContact",
                answers = List(Right("1234567890")),
                changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
              ),
              SummaryRow(
                id = "phoneNumberPAYEContact",
                answers = List(Right("0987654321")),
                changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
              ),
              SummaryRow(
                id = "correspondenceAddress",
                answers = formatHMTLCorrespondenceAddress,
                changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress())
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
        companyName = "Test Company",
        tradingName = Some("Test Company Trading Name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE4 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )

      val sicCodes = List(
        SICCode(
          code = None,
          description = Some("Novelty hairbrushes")
        )
      )

      val formatHMTLROAddress = service.addressToSummaryRowAnswers(validCompanyDetailsAPI.roAddress)
      val formatHMTLPPOBAddress = service.addressToSummaryRowAnswers(validCompanyDetailsAPI.ppobAddress)

      val companyDetailsSection = SummarySection(
        id = "companyDetails",
        Seq(
          SummaryRow(
            id = "tradingName",
            answers = List(Right("Test Company Trading Name")),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
          ),
          SummaryRow(
            id = "roAddress",
            answers = formatHMTLROAddress,
            None
          ),
          SummaryRow(
            id = "ppobAddress",
            answers = formatHMTLPPOBAddress,
            Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress())
          ),
          SummaryRow(
            id = "natureOfBusiness",
            answers = List(Right("Novelty hairbrushes")),
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
            answers = List(Left("false")),
            Some(controllers.userJourney.routes.EmploymentController.employingStaff())
          ),
          SummaryRow(
            id = "companyPension",
            answers = List(Left("false")),
            Some(controllers.userJourney.routes.EmploymentController.companyPension())
          ),
          SummaryRow(
            id = "subcontractors",
            answers = List(Left("false")),
            Some(controllers.userJourney.routes.EmploymentController.subcontractors())
          ),
          SummaryRow(
            id = "firstPaymentDate",
            answers = List(Right("20/12/2016")),
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
        DigitalContactDetails(
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
              answers = List(Right("test@email.com")),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              answers = List(Right("1234567890")),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              answers = List(Right("0987654321")),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            ))
          ).flatten
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) shouldBe validBCDSection
    }

    "return a summary section with no provided answers" in new Setup {
      val businessContactDetailsModel =
        DigitalContactDetails(
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
              answers = List(Left("noAnswerGiven")),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              answers = List(Left("noAnswerGiven")),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              answers = List(Left("noAnswerGiven")),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            ))
          ).flatten
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) shouldBe validBCDSection
    }
  }

  "buildContactDetails" should {
    "return a valid PAYE contact details block" in new Setup {
      val tstAddress = Address("tstLine1", "tstLine2", None, None, Some("pstCode"), Some("UK"))
      val tstContactDetails = PAYEContactDetails(
        name = "tstName",
        digitalContactDetails = DigitalContactDetails(
          Some("test@email.com"),
          Some("1234567890"),
          Some("0987654321")
        )
      )

      val tstContactSectionAPI = PAYEContactAPI(
        tstContactDetails,
        tstAddress
      )

      val validPAYEContactDetailsSection =
        SummarySection(
          id = "payeContactDetails",
          Seq(
            SummaryRow(
              id = "contactName",
              answers = List(Right("tstName")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "emailPAYEContact",
              answers = List(Right("test@email.com")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "mobileNumberPAYEContact",
              answers = List(Right("1234567890")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "phoneNumberPAYEContact",
              answers = List(Right("0987654321")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "correspondenceAddress",
              answers = List(Right("tstLine1"), Right("tstLine2"), Right("pstCode"), Right("UK")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress())
            )
          )
        )

      service.buildContactDetails(tstContactSectionAPI) shouldBe validPAYEContactDetailsSection
    }

    "return a valid PAYE contact details block with empty Digital Contact" in new Setup {
      val tstAddress = Address("tstLine1", "tstLine2", None, None, Some("pstCode"), Some("UK"))
      val tstContactDetails = PAYEContactDetails(
        name = "tstName",
        digitalContactDetails = DigitalContactDetails(
          None,None,None
        )
      )

      val tstContactSectionAPI = PAYEContactAPI(
        tstContactDetails,
        tstAddress
      )

      val validPAYEContactDetailsSection =
        SummarySection(
          id = "payeContactDetails",
          Seq(
            SummaryRow(
              id = "contactName",
              answers = List(Right("tstName")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "emailPAYEContact",
              answers = List(Left("noAnswerGiven")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "mobileNumberPAYEContact",
              answers = List(Left("noAnswerGiven")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "phoneNumberPAYEContact",
              answers = List(Left("noAnswerGiven")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
            ),
            SummaryRow(
              id = "correspondenceAddress",
              answers = List(Right("tstLine1"), Right("tstLine2"), Right("pstCode"), Right("UK")),
              changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress())
            )
          )
        )

      service.buildContactDetails(tstContactSectionAPI) shouldBe validPAYEContactDetailsSection
    }
  }

  "buildCompletionCapacitySection" should {
    "return a valid section for director" in new Setup {
      val capacity = "director"
      val section = SummarySection(
        id = "completionCapacity",
        Seq(
          SummaryRow(
            id ="completionCapacity",
            answers = List(Left("director")),
            changeLink = Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
          )
        )
      )

      service.buildCompletionCapacitySection(capacity) shouldBe section
    }

    "return a valid section for agent" in new Setup {
      val capacity = "agent"
      val section = SummarySection(
        id = "completionCapacity",
        Seq(
          SummaryRow(
            id ="completionCapacity",
            answers = List(Left("agent")),
            changeLink = Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
          )
        )
      )

      service.buildCompletionCapacitySection(capacity) shouldBe section
    }

    "return a valid section for other - Executive in charge of helicopters" in new Setup {
      val capacity = "Executive in charge of helicopters"
      val section = SummarySection(
        id = "completionCapacity",
        Seq(
          SummaryRow(
            id ="completionCapacity",
            answers = List(Right("Executive in charge of helicopters")),
            changeLink = Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
          )
        )
      )

      service.buildCompletionCapacitySection(capacity) shouldBe section
    }
  }

  "buildDirectorsSection" should {
    "return a valid director details block" in new Setup {
      val directorDetailsModel = List(
        Director(
          name = Name(
            forename = Some("Timothy"),
            otherForenames = Some("Potterley-Smythe"),
            surname = "Buttersford",
            title = Some("Mr")
          ),
          nino = Some("ZZ123456A")
        ),
        Director(
          name = Name(
            forename = Some("Pierre"),
            otherForenames = Some("Paul"),
            surname = "Simpson",
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
              answers = List(Right("ZZ 12 34 56 A")),
              changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
              questionArgs = Some(Seq("Timothy Buttersford")),
              commonQuestionKey = Some("director")
            )),
            Some(SummaryRow(
              id = "director1",
              answers = List(Right("")),
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
