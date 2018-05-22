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

package controllers.test

import enums.DownstreamOutcome
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TestRegSetupControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val request = FakeRequest()

  class Setup {
    val controller = new TestRegSetupController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService          = mockIncorpInfoService
      override val companyDetailsService      = mockCompanyDetailsService
      override val s4LService                 = mockS4LService
      override val testPAYERegConnector       = mockTestPayeRegConnector
      override val payeRegService             = mockPayeRegService
      override val messagesApi                = mockMessagesApi
      override val authConnector              = mockAuthConnector
      override val keystoreConnector          = mockKeystoreConnector
      override val testBusinessRegConnector   = mockTestBusRegConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result], payeRegistrationSubmitted: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          "12345",
          CompanyRegistrationProfile("held", "txId"),
          "ENG",
          payeRegistrationSubmitted = false,
          None
        ))
      }
    }
  }

  "regTeardown" should {
    "return an OK" when {
      "the registration collection has been successfully torn down" in new Setup {
        when(mockTestPayeRegConnector.testRegistrationTeardown()(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Success))

        AuthHelpers.showAuthorisedWithCP(controller.regTeardown, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe OK
        }
      }
    }

    "return an INTERNAL_SERVER_ERROR" when {
      "there was a problem tearing down the registration collection" in new Setup {
        when(mockTestPayeRegConnector.testRegistrationTeardown()(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Failure))

        AuthHelpers.showAuthorisedWithCP(controller.regTeardown, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "individualRegTeardown" should {
    "return an OK" when {
      "An existing registration is successfully deleted" in new Setup {
        when(mockTestPayeRegConnector.tearDownIndividualRegistration(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Success))

        AuthHelpers.showAuthorised(controller.individualRegTeardown("regID"), request) { result =>
          status(result) mustBe OK
        }
      }
    }

    "return an INTERNAL_SERVER_ERROR" when {
      "there was a problem deleting the registration document" in new Setup {
        when(mockTestPayeRegConnector.tearDownIndividualRegistration(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Failure))

        AuthHelpers.showAuthorised(controller.individualRegTeardown("regID"), request) { result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "regSetup" should {
    "return an OK" when {
      "the reg setup has been rendered with the form and the regId fetched" in new Setup {
        AuthHelpers.showAuthorisedWithCP(controller.regSetup, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe OK
        }
      }
    }
  }

  "submitRegSetup" should {
    "return a BAD_REQUEST" when {
      "there was a problem validating the form values" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody()

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetup, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "return an OK" when {
      "the form has been validated and the PAYE Reg document has been cached" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "registrationID" -> "testRegId",
          "transactionID" -> "10-1028374",
          "formCreationTimestamp" -> "2017-10-10T12:00:00Z",
          "completionCapacity" -> "testCapacity",
          "companyDetails.crn" -> "testCRN",
          "companyDetails.companyName" -> "testCompanyName",
          "companyDetails.tradingName" -> "testTradingName",
          "companyDetails.roAddress.line1" -> "testLine1",
          "companyDetails.roAddress.line2" -> "testLine2",
          "companyDetails.roAddress.line3" -> "testLine3",
          "companyDetails.roAddress.line4" -> "testLine4",
          "companyDetails.roAddress.postCode" -> "testPostCode",
          "companyDetails.roAddress.country" -> "testCountry",
          "companyDetails.ppobAddress.line1" -> "testLine1",
          "companyDetails.ppobAddress.line2" -> "testLine2",
          "companyDetails.ppobAddress.line3" -> "testLine3",
          "companyDetails.ppobAddress.line4" -> "testLine4",
          "companyDetails.ppobAddress.postCode" -> "testPostCode",
          "companyDetails.ppobAddress.country" -> "testCountry",
          "companyDetails.businessContactDetails.businessEmail" -> "test@email.com",
          "companyDetails.businessContactDetails.mobileNumber" -> "testNumber",
          "companyDetails.businessContactDetails.phoneNumber" -> "testNumber",
          "employment.employees" -> "true",
          "employment.companyPension" -> "true",
          "employment.subcontractors" -> "true",
          "employment.firstPayDateDay" -> "10",
          "employment.firstPayDateMonth" -> "10",
          "employment.firstPayDateYear" -> "2017",
          "sicCodes.code[0]" -> "1234567890",
          "sicCodes.description[0]" -> "Software",
          "directors[0].name.firstName" -> "testFirstName",
          "directors[0].name.middleName" -> "testMiddleName",
          "directors[0].name.lastName" -> "testLastName",
          "directors[0].name.title" -> "testTitle",
          "directors[0].nino" -> "testNino",
          "directors[1].name.firstName" -> "testFirstName",
          "directors[1].name.middleName" -> "testMiddleName",
          "directors[1].name.lastName" -> "testLastName",
          "directors[1].name.title" -> "testTitle",
          "directors[1].nino" -> "testNino",
          "directors[2].name.firstName" -> "testFirstName",
          "directors[2].name.middleName" -> "testMiddleName",
          "directors[2].name.lastName" -> "testLastName",
          "directors[2].name.title" -> "testTitle",
          "directors[2].nino" -> "testNino",
          "payeContact.payeContactDetails.name" -> "testName",
          "payeContact.payeContactDetails.digitalContactDetails.email" -> "test@email.com",
          "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "testNumber",
          "payeContact.payeContactDetails.digitalContactDetails.phoneNumber" -> "testNumber",
          "payeContact.correspondenceAddress.line1" -> "testLine1",
          "payeContact.correspondenceAddress.line2" -> "testLine2",
          "payeContact.correspondenceAddress.line3" -> "testLine3",
          "payeContact.correspondenceAddress.line4" -> "testLine4",
          "payeContact.correspondenceAddress.postCode" -> "testPostCode",
          "payeContact.correspondenceAddress.country" -> "testCountry"
        )

        when(mockTestPayeRegConnector.addPAYERegistration(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Success))

        when(mockTestBusRegConnector.updateCompletionCapacity(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful("director"))

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetup, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe OK
        }
      }
    }

    "return an INTERNAL_SERVER_ERROR" when {
      "the form has been validated but there was a problem caching the document" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "registrationID" -> "testRegId",
          "transactionID" -> "10-1028374",
          "formCreationTimestamp" -> "2017-10-10T12:00:00Z",
          "completionCapacity" -> "testCapacity",
          "companyDetails.crn" -> "testCRN",
          "companyDetails.companyName" -> "testCompanyName",
          "companyDetails.tradingName" -> "testTradingName",
          "companyDetails.roAddress.line1" -> "testLine1",
          "companyDetails.roAddress.line2" -> "testLine2",
          "companyDetails.roAddress.line3" -> "testLine3",
          "companyDetails.roAddress.line4" -> "testLine4",
          "companyDetails.roAddress.postCode" -> "testPostCode",
          "companyDetails.roAddress.country" -> "testCountry",
          "companyDetails.ppobAddress.line1" -> "testLine1",
          "companyDetails.ppobAddress.line2" -> "testLine2",
          "companyDetails.ppobAddress.line3" -> "testLine3",
          "companyDetails.ppobAddress.line4" -> "testLine4",
          "companyDetails.ppobAddress.postCode" -> "testPostCode",
          "companyDetails.ppobAddress.country" -> "testCountry",
          "companyDetails.businessContactDetails.businessEmail" -> "test@email.com",
          "companyDetails.businessContactDetails.mobileNumber" -> "testNumber",
          "companyDetails.businessContactDetails.phoneNumber" -> "testNumber",
          "employment.employees" -> "true",
          "employment.companyPension" -> "true",
          "employment.subcontractors" -> "true",
          "employment.firstPayDateDay" -> "10",
          "employment.firstPayDateMonth" -> "10",
          "employment.firstPayDateYear" -> "2017",
          "sicCodes.code[0]" -> "1234567890",
          "sicCodes.description[0]" -> "Software",
          "directors[0].name.firstName" -> "testFirstName",
          "directors[0].name.middleName" -> "testMiddleName",
          "directors[0].name.lastName" -> "testLastName",
          "directors[0].name.title" -> "testTitle",
          "directors[0].nino" -> "testNino",
          "directors[1].name.firstName" -> "testFirstName",
          "directors[1].name.middleName" -> "testMiddleName",
          "directors[1].name.lastName" -> "testLastName",
          "directors[1].name.title" -> "testTitle",
          "directors[1].nino" -> "testNino",
          "directors[2].name.firstName" -> "testFirstName",
          "directors[2].name.middleName" -> "testMiddleName",
          "directors[2].name.lastName" -> "testLastName",
          "directors[2].name.title" -> "testTitle",
          "directors[2].nino" -> "testNino",
          "payeContact.payeContactDetails.name" -> "testName",
          "payeContact.payeContactDetails.digitalContactDetails.email" -> "test@email.com",
          "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "testNumber",
          "payeContact.payeContactDetails.digitalContactDetails.phoneNumber" -> "testNumber",
          "payeContact.correspondenceAddress.line1" -> "testLine1",
          "payeContact.correspondenceAddress.line2" -> "testLine2",
          "payeContact.correspondenceAddress.line3" -> "testLine3",
          "payeContact.correspondenceAddress.line4" -> "testLine4",
          "payeContact.correspondenceAddress.postCode" -> "testPostCode",
          "payeContact.correspondenceAddress.country" -> "testCountry"
        )
        when(mockTestPayeRegConnector.addPAYERegistration(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Failure))

        when(mockTestBusRegConnector.updateCompletionCapacity(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful("director"))

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetup, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "regSetupCompanyDetails" should {
    "return an OK" when {
      "the payeRegCompanyDetailsSetup page has been rendered" in new Setup {
        AuthHelpers.showAuthorised(controller.regSetupCompanyDetails, request) { result =>
          status(result) mustBe OK
        }
      }
    }
  }

  "submitRegSetupCompanyDetails" should {
    "return a BAD_REQUEST" when {
      "the form values cannot be validated" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody()
        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetupCompanyDetails, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "return a OK" when {
      "the form data is valid and the test company details are cached" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "crn" -> "testCrn",
          "companyName" -> "testCompanyName",
          "tradingName" -> "testTradingName",
          "roAddress.line1" -> "testLine1",
          "roAddress.line2" -> "testLine2",
          "roAddress.line3" -> "testLine3",
          "roAddress.line4" -> "testLine4",
          "roAddress.postCode" -> "testPostCode",
          "roAddress.country" -> "testCountry",
          "ppobAddress.line1" -> "testLine1",
          "ppobAddress.line2" -> "testLine2",
          "ppobAddress.line3" -> "testLine3",
          "ppobAddress.line4" -> "testLine4",
          "ppobAddress.postCode" -> "testPostCode",
          "ppobAddress.country" -> "testCountry",
          "businessContactDetails.businessEmail" -> "test@email.com",
          "businessContactDetails.mobileNumber" -> "testNumber",
          "businessContactDetails.phoneNumber" -> "testNumber"
        )
        when(mockTestPayeRegConnector.addTestCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Success))

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetupCompanyDetails, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe OK
        }
      }
    }

    "return an INTERNAL_SERVER_ERROR" when {
      "the form data is valid but the test company details were not cached" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "crn" -> "testCrn",
          "companyName" -> "testCompanyName",
          "tradingName" -> "testTradingName",
          "roAddress.line1" -> "testLine1",
          "roAddress.line2" -> "testLine2",
          "roAddress.line3" -> "testLine3",
          "roAddress.line4" -> "testLine4",
          "roAddress.postCode" -> "testPostCode",
          "roAddress.country" -> "testCountry",
          "ppobAddress.line1" -> "testLine1",
          "ppobAddress.line2" -> "testLine2",
          "ppobAddress.line3" -> "testLine3",
          "ppobAddress.line4" -> "testLine4",
          "ppobAddress.postCode" -> "testPostCode",
          "ppobAddress.country" -> "testCountry",
          "businessContactDetails.businessEmail" -> "test@email.com",
          "businessContactDetails.mobileNumber" -> "testNumber",
          "businessContactDetails.phoneNumber" -> "testNumber"
        )
        when(mockTestPayeRegConnector.addTestCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Failure))

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetupCompanyDetails, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "regSetupPAYEContact" should {
    "return an OK" when {
      "the payeRegPAYEContactSetup page has been rendered" in new Setup {
        AuthHelpers.showAuthorised(controller.regSetupPAYEContact, request) { result =>
          status(result) mustBe OK
        }
      }
    }
  }

  "submitRegSetupPAYEContact" should {
    "return a BAD_REQUEST" when {
      "the form data cant be validated" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody()
        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetupPAYEContact, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "return an OK" when {
      "the form data has been validated and cached" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "payeContactDetails.name" -> "testName",
          "payeContactDetails.digitalContactDetails.email" -> "test@email.com",
          "payeContactDetails.digitalContactDetails.mobileNumber" -> "testNumber",
          "payeContactDetails.digitalContactDetails.phoneNumber" -> "testNumber",
          "correspondenceAddress.line1" -> "testLine1",
          "correspondenceAddress.line2" -> "testLine2",
          "correspondenceAddress.line3" -> "testLine3",
          "correspondenceAddress.line4" -> "testLine4",
          "correspondenceAddress.postCode" -> "testPostCode",
          "correspondenceAddress.country" -> "testCountry"
        )
        when(mockTestPayeRegConnector.addTestPAYEContact(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Success))

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetupPAYEContact, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe OK
        }
      }
    }

    "return an INTERNAL_SERVER_ERROR" when {
      "the form data has been validated but not cached" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "payeContactDetails.name" -> "testName",
          "payeContactDetails.digitalContactDetails.email" -> "test@email.com",
          "payeContactDetails.digitalContactDetails.mobileNumber" -> "testNumber",
          "payeContactDetails.digitalContactDetails.phoneNumber" -> "testNumber",
          "correspondenceAddress.line1" -> "testLine1",
          "correspondenceAddress.line2" -> "testLine2",
          "correspondenceAddress.line3" -> "testLine3",
          "correspondenceAddress.line4" -> "testLine4",
          "correspondenceAddress.postCode" -> "testPostCode",
          "correspondenceAddress.country" -> "testCountry"
        )
        when(mockTestPayeRegConnector.addTestPAYEContact(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(DownstreamOutcome.Failure))

        AuthHelpers.submitAuthorisedWithCP(controller.submitRegSetupPAYEContact, Fixtures.validCurrentProfile, request) { result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
