/*
 * Copyright 2022 HM Revenue & Customs
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
import models.SummaryListRowUtils.optSummaryListRowSeq
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
            surname = Some("Buttersford"),
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
          surname = Some("Buttersford"),
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
    "return None when the connector returns a Forbidden response" in new Setup {
      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(forbidden))

      intercept[Upstream4xxResponse](await(service.getEmploymentSectionSummary("45632", "fooBar")))
    }

    "return None when the connector returns a Not Found response" in new Setup {
      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(notFound))

      intercept[NotFoundException](await(service.getEmploymentSectionSummary("45632", "fooBar")))
    }

    "return None when the connector returns an exception response" in new Setup {
      when(mockPAYERegConnector.getRegistration(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(runTimeException))

      intercept[RuntimeException](await(service.getEmploymentSectionSummary("45632", "fooBar")))
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

      val tradingName = optSummaryListRowSeq(
        messages("pages.summary.tradingName.question"),
        Some(Seq("Test Company Trading Name")),
        Some(controllers.userJourney.routes.CompanyDetailsController.tradingName.url)
      )

      val roAddress = optSummaryListRowSeq(
        messages("pages.summary.roAddress.question"),
        Some(formatHMTLROAddress),
        None
      )

      val ppobAddress = optSummaryListRowSeq(
        messages("pages.summary.ppobAddress.question"),
        Some(formatHMTLPPOBAddress),
        Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress.url)
      )

      val natureOfBusiness = optSummaryListRowSeq(
        messages("pages.summary.natureOfBusiness.question"),
        Some(Seq("Novelty hairbrushes")),
        Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness.url)
      )

      val companyDetailsSection = Seq(
        tradingName,
        roAddress,
        ppobAddress,
        natureOfBusiness
      ).flatten

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
    "return a valid business contact details block" in new Setup {
      val businessContactDetailsModel =
        DigitalContactDetails(
          Some("test@email.com"),
          Some("1234567890"),
          Some("0987654321")
        )

      val businessEmail = optSummaryListRowSeq(
        messages("pages.summary.businessEmail.question"),
        Some(Seq("test@email.com")),
        Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
      )

      val mobileNumber = optSummaryListRowSeq(
        messages("pages.summary.mobileNumber.question"),
        Some(Seq("1234567890")),
        Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
      )

      val businessTelephone = optSummaryListRowSeq(
        messages("pages.summary.businessTelephone.question"),
        Some(Seq("0987654321")),
        Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
      )

      val validBCDSection = Seq(
        businessEmail,
        mobileNumber,
        businessTelephone
      ).flatten

      service.buildBusinessContactDetailsSection(businessContactDetailsModel) mustBe validBCDSection
    }

    "return a summary section with no provided answers" in new Setup {
      val businessContactDetailsModel =
        DigitalContactDetails(
          None,
          None,
          None
        )

      val businessEmail = optSummaryListRowSeq(
        messages("pages.summary.businessEmail.question"),
        Some(Seq("")),
        Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
      )

      val mobileNumber = optSummaryListRowSeq(
        messages("pages.summary.mobileNumber.question"),
        Some(Seq("")),
        Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
      )

      val businessTelephone = optSummaryListRowSeq(
        messages("pages.summary.businessTelephone.question"),
        Some(Seq("")),
        Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
      )

      val validBCDSection = Seq(
        businessEmail,
        mobileNumber,
        businessTelephone
      ).flatten

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

      val contactName = optSummaryListRowSeq(
        messages("pages.summary.contactName.question"),
        Some(Seq("tstName")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val emailContact = optSummaryListRowSeq(
        messages("pages.summary.emailPAYEContact.question"),
        Some(Seq("test@email.com")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val mobileContact = optSummaryListRowSeq(
        messages("pages.summary.mobileNumberPAYEContact.question"),
        Some(Seq("1234567890")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val phoneNumber = optSummaryListRowSeq(
        messages("pages.summary.phoneNumberPAYEContact.question"),
        Some(Seq("0987654321")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val correspondanceAddress = optSummaryListRowSeq(
        messages("pages.summary.correspondenceAddress.question"),
        Some(Seq("tstLine1", "tstLine2", "pstCode", "UK")),
        Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress.url)
      )

      val validPAYEContactDetailsSection = Seq(
        contactName,
        emailContact,
        mobileContact,
        phoneNumber,
        correspondanceAddress
      ).flatten

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

      val contactName = optSummaryListRowSeq(
        messages("pages.summary.contactName.question"),
        Some(Seq("tstName")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val emailContact = optSummaryListRowSeq(
        messages("pages.summary.emailPAYEContact.question"),
        Some(Seq("")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val mobileContact = optSummaryListRowSeq(
        messages("pages.summary.mobileNumberPAYEContact.question"),
        Some(Seq("")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val phoneNumber = optSummaryListRowSeq(
        messages("pages.summary.phoneNumberPAYEContact.question"),
        Some(Seq("")),
        Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
      )

      val correspondanceAddress = optSummaryListRowSeq(
        messages("pages.summary.correspondenceAddress.question"),
        Some(Seq("tstLine1", "tstLine2", "pstCode", "UK")),
        Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress.url)
      )

      val validPAYEContactDetailsSection = Seq(
        contactName,
        emailContact,
        mobileContact,
        phoneNumber,
        correspondanceAddress
      ).flatten

      service.buildContactDetails(tstContactSectionAPI) mustBe validPAYEContactDetailsSection
    }
  }

  "buildCompletionCapacitySection" should {
    "return a valid section for director" in new Setup {
      val capacity = "director"

      val completionCapacity = optSummaryListRowSeq(
        messages("pages.summary.completionCapacity.question"),
        Some(Seq("Director")),
        Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      )

      val completionCapacitySectionBuilder = Seq(
        completionCapacity
      ).flatten

      service.buildCompletionCapacitySection(capacity) mustBe completionCapacitySectionBuilder
    }

    "return a valid section for agent" in new Setup {
      val capacity = "agent"

      val completionCapacity = optSummaryListRowSeq(
        messages("pages.summary.completionCapacity.question"),
        Some(Seq("Agent")),
        Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      )

      val completionCapacitySectionBuilder = Seq(
        completionCapacity
      ).flatten

      service.buildCompletionCapacitySection(capacity) mustBe completionCapacitySectionBuilder
    }

    "return a valid section for other - Executive in charge of helicopters" in new Setup {
      val capacity = "Executive in charge of helicopters"

      val completionCapacity = optSummaryListRowSeq(
        messages("pages.summary.completionCapacity.question"),
        Some(Seq("Executive in charge of helicopters")),
        Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      )

      val completionCapacitySectionBuilder = Seq(
        completionCapacity
      ).flatten

      service.buildCompletionCapacitySection(capacity) mustBe completionCapacitySectionBuilder
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
        )
      )


      val validDirectorDetailsSection =
        optSummaryListRowSeq(
          "Timothy Buttersford’s National Insurance number",
          Some(Seq("ZZ 12 34 56 A")),
          Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails.url)
        )

      val DirectorDetailsSectionBuilder = Seq(
        validDirectorDetailsSection
      ).flatten

      service.buildDirectorsSection(directorDetailsModel) mustBe DirectorDetailsSectionBuilder
    }
  }
}