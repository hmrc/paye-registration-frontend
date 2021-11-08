/*
 * Copyright 2021 HM Revenue & Customs
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
import enums.PAYEStatus
import helpers.PayeComponentSpec
import models.api.{Director, Name, SICCode, CompanyDetails => CompanyDetailsAPI, PAYEContact => PAYEContactAPI, PAYERegistration => PAYERegistrationAPI}
import models.view._
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{NotFoundException, Upstream4xxResponse}

import java.time.LocalDate
import scala.concurrent.Future

class SummaryServiceSpec extends PayeComponentSpec with GuiceOneAppPerSuite {

  class Setup(enabled: Boolean = false) {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

    val service = new SummaryService(
      mockPAYERegConnector,
      mockEmploymentService,
      mockIncorpInfoService,
      app.injector.instanceOf[MessagesApi]
    )

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
        businessContactDetails = Fixtures.validBusinessContactDetails
      ),
      employmentInfo = Fixtures.validEmploymentApi,
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
      payeContact = Fixtures.validPAYEContactAPI
    )

    val formatHMTLROAddress = service.addressToSummaryRowAnswers(apiRegistrationNoTName.companyDetails.roAddress)
    val formatHMTLPPOBAddress = service.addressToSummaryRowAnswers(apiRegistrationNoTName.companyDetails.ppobAddress)
    val formatHMTLCorrespondenceAddress = service.addressToSummaryRowAnswers(Fixtures.validPAYEContactAPI.correspondenceAddress)

    val employeeAPISS: SummarySection = SummarySection(
      id = "employees",
      sectionHeading = "Employment information",
      Seq(
        SummaryRow(
          id = "employing",
          question = "Does the company employ anyone or provide expenses or benefits to staff?",
          answers = List("No"),
          Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.paidEmployees(), "if the company employs anyone or provides expenses or benefits to staff."))
        ),
        SummaryRow(
          id = "earliestDate",
          question = "When did it first start employing someone or providing expenses or benefits to staff?",
          answers = List("20/12/2016"),
          Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.paidEmployees(), "when the company first started employing someone or providing expenses or benefits to staff."))
        ),
        SummaryRow(
          id = "inConstructionIndustry",
          question = "Does the company work in the construction industry?",
          answers = List("Yes"),
          Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.constructionIndustry(), "whether or not the company works in the construction industry."))
        ),
        SummaryRow(
          id = "paysPension",
          question = "Does the company make pension payments to a former employee or their dependants?",
          answers = List("Yes"),
          Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.pensions(), "if the company makes pension payments to a former employee or their dependants."))
        )
      )
    )

    val completionCapacitySS: SummarySection = SummarySection(
      id = "completionCapacity",
      sectionHeading = "About you",
      Seq(
        SummaryRow(
          id = "completionCapacity",
          question = "What is your relationship to the company?",
          answers = Seq("High Priestess"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompletionCapacityController.completionCapacity(), "your relationship to the company."))
        )
      )
    )

    val companyDetailsSS: SummarySection = SummarySection(
      id = "companyDetails",
      sectionHeading = "Company information",
      Seq(
        SummaryRow(
          id = "tradingName",
          question = "Does or will the company trade using a different name?",
          answers = Seq("No"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.tradingName(), "whether your company will trade under another name."))
        ),
        SummaryRow(
          id = "roAddress",
          question = "What is your company's registered office address?",
          answers = formatHMTLROAddress,
          None
        ),
        SummaryRow(
          id = "ppobAddress",
          question = "Where will the company carry out most of its business activities?",
          answers = formatHMTLPPOBAddress,
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.ppobAddress(), "where your company will carry out most of its business activities."))
        ),
        SummaryRow(
          id = "natureOfBusiness",
          question = "What does your company do?",
          answers = Seq("Firearms"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness(), "what your company does."))
        )
      )
    )

    val businessContactDetailsSS: SummarySection = SummarySection(
      id = "businessContactDetails",
      sectionHeading = "Company contact details",
      Seq(
        SummaryRow(
          id = "businessEmail",
          question = "What is the company contact's email address?",
          answers = Seq("test@email.com"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's email address."))
        ),
        SummaryRow(
          id = "mobileNumber",
          question = "What is the company contact's number?",
          answers = Seq("1234567890"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's number."))
        ),
        SummaryRow(
          id = "businessTelephone",
          question = "What is the company contact's other number?",
          answers = Seq("0987654321"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's other number."))
        )
      )
    )

    val directorDetailsSS = SummarySection(
      id = "directorDetails",
      sectionHeading = "Director details",
      Seq(
        SummaryRow(
          id = "director0",
          question = "Timothy Buttersford's National Insurance number",
          answers = List("ZZ 12 34 56 A"),
          Some(SummaryChangeLink(controllers.userJourney.routes.DirectorDetailsController.directorDetails(), "Timothy Buttersford's National Insurance number"))
        )
      )
    )

    val payeContactDetailsSS = SummarySection(
      id = "payeContactDetails",
      sectionHeading = "PAYE contact details",
      Seq(
        SummaryRow(
          id = "contactName",
          question = "What is the name of the company's PAYE contact?",
          answers = Seq("testName"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the name of the company's PAYE contact."))
        ),
        SummaryRow(
          id = "emailPAYEContact",
          question = "What is the email address of the company's PAYE contact?",
          answers = Seq("testEmail"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the email address of the company's PAYE contact."))
        ),
        SummaryRow(
          id = "mobileNumberPAYEContact",
          question = "What is the contact number of the company's PAYE contact?",
          answers = Seq("1234567890"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the contact number of the company's PAYE contact."))
        ),
        SummaryRow(
          id = "phoneNumberPAYEContact",
          question = "What is the other contact number of the company's PAYE contact?",
          answers = Seq("0987654321"),
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the other contact number of the company's PAYE contact."))
        ),
        SummaryRow(
          id = "correspondenceAddress",
          question = "Where should we send PAYE-related post to?",
          answers = formatHMTLCorrespondenceAddress,
          optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress(), "where we should send PAYE-related post to."))
        )
      )
    )

    def generateSummarySections(employee: SummarySection = employeeAPISS,
                                cc: SummarySection = completionCapacitySS,
                                compDets: SummarySection = companyDetailsSS,
                                bcDets: SummarySection = businessContactDetailsSS,
                                dirDets: SummarySection = directorDetailsSS,
                                payeCDets: SummarySection = payeContactDetailsSS): Summary = {
      Summary(
        Seq(
          employee,
          cc,
          compDets,
          bcDets,
          dirDets,
          payeCDets
        )
      )
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
      businessContactDetails = Fixtures.validBusinessContactDetails
    ),
    employmentInfo = Fixtures.validEmploymentApi,
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
    payeContact = Fixtures.validPAYEContactAPI
  )

  val forbidden = Upstream4xxResponse("403", 403, 403)
  val notFound = new NotFoundException("404")
  val runTimeException = new RuntimeException("tst")

  "Calling getRegistrationSummary" should {
    "return a defined Summary when the connector returns a valid PAYE Registration response" in new Setup {
      lazy val summaryNoTName = generateSummarySections(employee =
        SummarySection(
          id = "employees",
          sectionHeading = "Employment information",
          Seq(
            SummaryRow(
              id = "willBePaying",
              question = "Over the next 2 months will the company employ anyone or provide expenses or benefits to staff?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.employingStaff(), "if the company will employ anyone or provide expenses or benefits to staff, over the next 2 months."))
            ),
            SummaryRow(
              id = "inConstructionIndustry",
              question = "Does the company work in the construction industry?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.constructionIndustry(), "whether or not the company works in the construction industry."))
            ),
            SummaryRow(
              id = "employsSubcontractors",
              question = "During the current tax year will the company hire any subcontractors in the construction industry?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.subcontractors(), "if the company will hire any subcontractors in the construction industry during the current tax year."))
            )
          )
        )
      )

      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(apiRegistrationNoTName))
      when(mockEmploymentService.apiToView(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Fixtures.validEmploymentViewNotIncorporated)
      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      await(service.getRegistrationSummary("45632", "fooBar")) mustBe summaryNoTName
    }

    "return None when the connector returns a Forbidden response" in new Setup {
      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(forbidden))

      intercept[Upstream4xxResponse](await(service.getRegistrationSummary("45632", "fooBar")))
    }

    "return None when the connector returns a Not Found response" in new Setup {
      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(notFound))

      intercept[NotFoundException](await(service.getRegistrationSummary("45632", "fooBar")))
    }

    "return None when the connector returns an exception response" in new Setup {
      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(runTimeException))

      intercept[RuntimeException](await(service.getRegistrationSummary("45632", "fooBar")))
    }
  }

  "getRegistrationSummary valid test" should {
    "convert a PAYE Registration API Model to a Summary model with feature switch on for new Employment Block. incorped and trading name does not exist" in new Setup(true) {

      lazy val summaryNoTName = generateSummarySections(employee =
        SummarySection(
          id = "employees",
          sectionHeading = "Employment information",
          Seq(
            SummaryRow(
              id = "employing",
              question = "Does the company employ anyone or provide expenses or benefits to staff?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.paidEmployees(), "if the company employs anyone or provides expenses or benefits to staff."))
            ),
            SummaryRow(
              id = "earliestDate",
              question = "When did it first start employing someone or providing expenses or benefits to staff?",
              answers = Seq("20/12/2016"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.paidEmployees(), "when the company first started employing someone or providing expenses or benefits to staff."))
            ),
            SummaryRow(
              id = "inConstructionIndustry",
              question = "Does the company work in the construction industry?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.constructionIndustry(), "whether or not the company works in the construction industry."))
            ),
            SummaryRow(
              id = "employsSubcontractors",
              question = "During the current tax year will the company hire any subcontractors in the construction industry?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.subcontractors(), "if the company will hire any subcontractors in the construction industry during the current tax year."))
            ),
            SummaryRow(
              id = "paysPension",
              question = "Does the company make pension payments to a former employee or their dependants?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.pensions(), "if the company makes pension payments to a former employee or their dependants."))
            )
          )
        )
      )

      when(mockEmploymentService.apiToView(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Fixtures.validEmploymentViewIncorporated)

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.now)))

      service.registrationToSummary(apiRegistrationNoTName, "regId", Some(LocalDate.now)) mustBe summaryNoTName
    }
    "convert a PAYE Registration API Model to a Summary model with feature switch on for new Employment Block. incorped and trading name exists" in new Setup(true) {
      lazy val companyDetailsWithTradingNameSS = SummarySection(
        id = "companyDetails",
        sectionHeading = "Company information",
        Seq(
          SummaryRow(
            id = "tradingName",
            question = "Does or will the company trade using a different name?",
            answers = Seq("foo"),
            optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.tradingName(), "whether your company will trade under another name."))
          ),
          SummaryRow(
            id = "roAddress",
            question = "What is your company's registered office address?",
            answers = formatHMTLROAddress,
            None
          ),
          SummaryRow(
            id = "ppobAddress",
            question = "Where will the company carry out most of its business activities?",
            answers = formatHMTLPPOBAddress,
            Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.ppobAddress(), "where your company will carry out most of its business activities."))
          ),
          SummaryRow(
            id = "natureOfBusiness",
            question = "What does your company do?",
            answers = Seq("Firearms"),
            Some(SummaryChangeLink(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness(), "what your company does."))
          )
        )
      )

      lazy val SummaryTradingName = generateSummarySections(
        compDets = companyDetailsWithTradingNameSS,
        employee =
          SummarySection(
            id = "employees",
            sectionHeading = "Employment information",
            Seq(
              SummaryRow(
                id = "employing",
                question = "Does the company employ anyone or provide expenses or benefits to staff?",
                answers = Seq("Yes"),
                Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.paidEmployees(), "if the company employs anyone or provides expenses or benefits to staff."))
              ),
              SummaryRow(
                id = "earliestDate",
                question = "When did it first start employing someone or providing expenses or benefits to staff?",
                answers = Seq("20/12/2016"),
                Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.paidEmployees(), "when the company first started employing someone or providing expenses or benefits to staff."))
              ),
              SummaryRow(
                id = "inConstructionIndustry",
                question = "Does the company work in the construction industry?",
                answers = Seq("Yes"),
                Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.constructionIndustry(), "whether or not the company works in the construction industry."))
              ),
              SummaryRow(
                id = "employsSubcontractors",
                question = "During the current tax year will the company hire any subcontractors in the construction industry?",
                answers = Seq("Yes"),
                Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.subcontractors(), "if the company will hire any subcontractors in the construction industry during the current tax year."))
              ),
              SummaryRow(
                id = "paysPension",
                question = "Does the company make pension payments to a former employee or their dependants?",
                answers = Seq("Yes"),
                Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.pensions(), "if the company makes pension payments to a former employee or their dependants."))
              )
            )
          )
      )

      when(mockEmploymentService.apiToView(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Fixtures.validEmploymentViewIncorporated)

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.now)))

      service.registrationToSummary(
        apiRegistrationNoTName.copy(companyDetails = apiRegistrationNoTName.companyDetails.copy(tradingName = Some("foo"))),
        "regId",
        Some(LocalDate.now)
      ) mustBe SummaryTradingName
    }
    "convert a PAYE Registration API Model to a Summary model with feature switch on for new Employment Block that is not incorped" in new Setup(true) {

      lazy val summaryNoTName = generateSummarySections(employee =
        SummarySection(
          id = "employees",
          sectionHeading = "Employment information",
          Seq(
            SummaryRow(
              id = "willBePaying",
              question = "Over the next 2 months will the company employ anyone or provide expenses or benefits to staff?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.employingStaff(), "if the company will employ anyone or provide expenses or benefits to staff, over the next 2 months."))
            ),
            SummaryRow(
              id = "inConstructionIndustry",
              question = "Does the company work in the construction industry?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.constructionIndustry(), "whether or not the company works in the construction industry."))
            ),
            SummaryRow(
              id = "employsSubcontractors",
              question = "During the current tax year will the company hire any subcontractors in the construction industry?",
              answers = Seq("Yes"),
              Some(SummaryChangeLink(controllers.userJourney.routes.EmploymentController.subcontractors(), "if the company will hire any subcontractors in the construction industry during the current tax year."))
            )
          )
        )
      )

      when(mockEmploymentService.apiToView(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Fixtures.validEmploymentViewNotIncorporated)
      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      service.registrationToSummary(apiRegistrationNoTName, "regId", None) mustBe summaryNoTName
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

      override val formatHMTLROAddress = service.addressToSummaryRowAnswers(validCompanyDetailsAPI.roAddress)
      override val formatHMTLPPOBAddress = service.addressToSummaryRowAnswers(validCompanyDetailsAPI.ppobAddress)

      val companyDetailsSection = SummarySection(
        id = "companyDetails",
        sectionHeading = "Company information",
        Seq(
          SummaryRow(
            id = "tradingName",
            question = "Does or will the company trade using a different name?",
            answers = Seq("Test Company Trading Name"),
            optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.tradingName(), "whether your company will trade under another name."))
          ),
          SummaryRow(
            id = "roAddress",
            question = "What is your company's registered office address?",
            answers = formatHMTLROAddress,
            None
          ),
          SummaryRow(
            id = "ppobAddress",
            question = "Where will the company carry out most of its business activities?",
            answers = formatHMTLPPOBAddress,
            Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.ppobAddress(), "where your company will carry out most of its business activities."))
          ),
          SummaryRow(
            id = "natureOfBusiness",
            question = "What does your company do?",
            answers = Seq("Novelty hairbrushes"),
            Some(SummaryChangeLink(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness(), "what your company does."))
          )
        )
      )
      service.buildCompanyDetailsSection(validCompanyDetailsAPI, sicCodes) mustBe companyDetailsSection
    }

    "throw the correct exception when there is no description provided" in new Setup {
      val sicCodesModel = List(
        SICCode(
          code = None,
          description = None
        )
      )

      an[APIConversionException] mustBe thrownBy(service.buildCompanyDetailsSection(Fixtures.validCompanyDetailsAPI, sicCodesModel))
    }

    "throw the correct exception when there is no SIC code provided" in new Setup {
      val sicCodesModel = List.empty

      a[NoSuchElementException] mustBe thrownBy(service.buildCompanyDetailsSection(Fixtures.validCompanyDetailsAPI, sicCodesModel))
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
          sectionHeading = "Company contact details",
          Seq(
            Some(SummaryRow(
              id = "businessEmail",
              question = "What is the company contact's email address?",
              answers = Seq("test@email.com"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's email address."))
            )),
            Some(SummaryRow(
              id = "mobileNumber",
              question = "What is the company contact's number?",
              answers = Seq("1234567890"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's number."))
            )),
            Some(SummaryRow(
              id = "businessTelephone",
              question = "What is the company contact's other number?",
              answers = Seq("0987654321"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's other number."))
            ))
          ).flatten
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) mustBe validBCDSection
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
          sectionHeading = "Company contact details",
          Seq(
            SummaryRow(
              id = "businessEmail",
              question = "What is the company contact's email address?",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's email address."))
            ),
            SummaryRow(
              id = "mobileNumber",
              question = "What is the company contact's number?",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's number."))
            ),
            SummaryRow(
              id = "businessTelephone",
              question = "What is the company contact's other number?",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails(), "the company contact's other number."))
            )
          )
        )

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) mustBe validBCDSection
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
          sectionHeading = "PAYE contact details",
          Seq(
            SummaryRow(
              id = "contactName",
              question = "What is the name of the company's PAYE contact?",
              answers = Seq("tstName"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the name of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "emailPAYEContact",
              question = "What is the email address of the company's PAYE contact?",
              answers = Seq("test@email.com"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the email address of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "mobileNumberPAYEContact",
              question = "What is the contact number of the company's PAYE contact?",
              answers = Seq("1234567890"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the contact number of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "phoneNumberPAYEContact",
              question = "What is the other contact number of the company's PAYE contact?",
              answers = Seq("0987654321"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the other contact number of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "correspondenceAddress",
              question = "Where should we send PAYE-related post to?",
              answers = Seq("tstLine1", "tstLine2", "pstCode", "UK"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress(), "where we should send PAYE-related post to."))
            )
          )
        )

      service.buildContactDetails(tstContactSectionAPI) mustBe validPAYEContactDetailsSection
    }

    "return a valid PAYE contact details block with empty Digital Contact" in new Setup {
      val tstAddress = Address("tstLine1", "tstLine2", None, None, Some("pstCode"), Some("UK"))
      val tstContactDetails = PAYEContactDetails(
        name = "tstName",
        digitalContactDetails = DigitalContactDetails(
          None, None, None
        )
      )

      val tstContactSectionAPI = PAYEContactAPI(
        tstContactDetails,
        tstAddress
      )

      val validPAYEContactDetailsSection =
        SummarySection(
          id = "payeContactDetails",
          sectionHeading = "PAYE contact details",
          Seq(
            SummaryRow(
              id = "contactName",
              question = "What is the name of the company's PAYE contact?",
              answers = Seq("tstName"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the name of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "emailPAYEContact",
              question = "What is the email address of the company's PAYE contact?",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the email address of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "mobileNumberPAYEContact",
              question = "What is the contact number of the company's PAYE contact?",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the contact number of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "phoneNumberPAYEContact",
              question = "What is the other contact number of the company's PAYE contact?",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeContactDetails(), "the other contact number of the company's PAYE contact."))
            ),
            SummaryRow(
              id = "correspondenceAddress",
              question = "Where should we send PAYE-related post to?",
              answers = Seq("tstLine1", "tstLine2", "pstCode", "UK"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress(), "where we should send PAYE-related post to."))
            )
          )
        )

      service.buildContactDetails(tstContactSectionAPI) mustBe validPAYEContactDetailsSection
    }
  }

  "buildCompletionCapacitySection" should {
    "return a valid section for director" in new Setup {
      val capacity = "director"
      val section = SummarySection(
        id = "completionCapacity",
        sectionHeading = "About you",
        Seq(
          SummaryRow(
            id = "completionCapacity",
            question = "What is your relationship to the company?",
            answers = Seq("Director"),
            optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompletionCapacityController.completionCapacity(), "your relationship to the company."))
          )
        )
      )

      service.buildCompletionCapacitySection(capacity) mustBe section
    }

    "return a valid section for agent" in new Setup {
      val capacity = "agent"
      val section = SummarySection(
        id = "completionCapacity",
        sectionHeading = "About you",
        Seq(
          SummaryRow(
            id = "completionCapacity",
            question = "What is your relationship to the company?",
            answers = Seq("Agent"),
            optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompletionCapacityController.completionCapacity(), "your relationship to the company."))
          )
        )
      )

      service.buildCompletionCapacitySection(capacity) mustBe section
    }

    "return a valid section for other - Executive in charge of helicopters" in new Setup {
      val capacity = "Executive in charge of helicopters"
      val section = SummarySection(
        id = "completionCapacity",
        sectionHeading = "About you",
        Seq(
          SummaryRow(
            id = "completionCapacity",
            question = "What is your relationship to the company?",
            answers = Seq("Executive in charge of helicopters"),
            optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.CompletionCapacityController.completionCapacity(), "your relationship to the company."))
          )
        )
      )

      service.buildCompletionCapacitySection(capacity) mustBe section
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


      val validDirectorDetailsSection: SummarySection =
        SummarySection(
          id = "directorDetails",
          sectionHeading = "Director details",
          Seq(
            SummaryRow(
              id = "director0",
              question = "Timothy Buttersford's National Insurance number",
              answers = Seq("ZZ 12 34 56 A"),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.DirectorDetailsController.directorDetails(), "Timothy Buttersford's National Insurance number"))
            ),
            SummaryRow(
              id = "director1",
              question = "Pierre Simpson's National Insurance number",
              answers = Seq(""),
              optChangeLink = Some(SummaryChangeLink(controllers.userJourney.routes.DirectorDetailsController.directorDetails(), "Pierre Simpson's National Insurance number"))
            )
          )
        )

      service.buildDirectorsSection(directorDetailsModel) mustBe validDirectorDetailsSection
    }
  }
}