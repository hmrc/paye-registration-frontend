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

package controllers.userJourney

import enums.DownstreamOutcome
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.Address
import models.external.AuditingInformation
import models.view.PAYEContactDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{AnyContent, Call, Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PAYEContactControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val regId = "12345"

  class Setup {
    val testController = new PAYEContactController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService      = mockIncorpInfoService
      override val s4LService             = mockS4LService
      override val companyDetailsService  = mockCompanyDetailsService
      override val payeContactService     = mockPAYEContactService
      override val addressLookupService   = mockAddressLookupService
      override val keystoreConnector      = mockKeystoreConnector
      override val messagesApi            = mockMessagesApi
      override val authConnector          = mockAuthConnector
      override val prepopService          = mockPrepopService
      override val auditService           = mockAuditService
    }
  }

  val request = FakeRequest()

  "payeContactDetails" should {
    "return an OK with data from registration" in new Setup {
      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future(Fixtures.validPAYEContactView))

      AuthHelpers.showAuthorisedWithCP(testController.payeContactDetails, Fixtures.validCurrentProfile, request) {
        (result: Future[Result])  =>
          status(result) mustBe OK
      }
    }

    "return an OK with data from prepopulation" in new Setup {
      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.emptyPAYEContactView))

      AuthHelpers.showAuthorisedWithCP(testController.payeContactDetails, Fixtures.validCurrentProfile, request) {
        (result: Future[Result])  =>
          status(result) mustBe OK
      }
    }

    "return an OK without data" in new Setup {
      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.emptyPAYEContactView))

      when(mockPrepopService.getPAYEContactDetails(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future(None))

      AuthHelpers.showAuthorisedWithCP(testController.payeContactDetails, Fixtures.validCurrentProfile, request) {
        (result: Future[Result])  =>
          status(result) mustBe OK
      }
    }
  }

  "submitPAYEContactDetails" should {
    "return a BAD_REQUEST if there is problem with the submitted form, no name" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> ""
      )

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYEContactDetails, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form, no contact details" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "teeeeeeest"
      )

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYEContactDetails, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "tata",
        "digitalContact.contactEmail" -> "tata@test.com"
      )

      when(mockPAYEContactService.submitPayeContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYEContactDetails, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return a SEE_OTHER and redirect to the Correspondence Address page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "tata",
        "digitalContact.contactEmail" -> "tata@test.com"
      )

      when(mockPAYEContactService.submitPayeContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveContactDetails(ArgumentMatchers.eq(regId), ArgumentMatchers.any[PAYEContactDetails]())(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYEContactDetails, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/where-to-send-post")
      }
    }
  }

  "payeCorrespondenceAddress" should {
    "return an OK" in new Setup {
      val addressMap = Map(
        "ro" -> Fixtures.validCompanyDetailsViewModel.roAddress,
        "correspondence" -> Fixtures.validCompanyDetailsViewModel.ppobAddress.get
      )

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validPAYEContactView))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPrepopService.getPrePopAddresses(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Map.empty[Int, Address]))

      when(mockPAYEContactService.getCorrespondenceAddresses(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(addressMap)

      AuthHelpers.showAuthorisedWithCP(testController.payeCorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe OK
      }
    }
  }

  "submitPAYECorrespondenceAddress" should {
    "return a BAD_REQUEST" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> ""
      )

      val addressMap = Map(
        "ro"             -> Fixtures.validCompanyDetailsViewModel.roAddress,
        "correspondence" -> Fixtures.validCompanyDetailsViewModel.ppobAddress.get
      )

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYEContactView))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPrepopService.getPrePopAddresses(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(Map(1 ->Fixtures.validCompanyDetailsViewModel.roAddress)))

      when(mockPAYEContactService.getCorrespondenceAddresses(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(addressMap)

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to summary if correspondence is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "correspondenceAddress"
      )

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/register-for-paye/check-and-confirm-your-answers")
      }
    }

    "redirect to summary if ro is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "roAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockAuditService.auditCorrespondenceAddress(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[AuditingInformation](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[AnyContent]]()))
        .thenReturn(Future.successful(AuditResult.Success))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/register-for-paye/check-and-confirm-your-answers")
      }
    }

    "redirect to summary if ppob is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "ppobAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockAuditService.auditCorrespondenceAddress(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[AuditingInformation](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[AnyContent]]()))
        .thenReturn(Future.successful(AuditResult.Success))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/register-for-paye/check-and-confirm-your-answers")
      }
    }

    "return a 500 in case of DownstreamOutcome FAILURE when saving with a missing PPOB Address" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "ppobAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel.copy(ppobAddress = None)))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return a 500 in case of DownstreamOutcome FAILURE" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "roAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockAuditService.auditCorrespondenceAddress(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[AuditingInformation](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[AnyContent]]()))
        .thenReturn(Future.successful(AuditResult.Success))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to address lookup frontend if other is chosen" in new Setup {
      val request = FakeRequest("GET", "/testuri?id=123456789").withFormUrlEncodedBody(
        "chosenAddress" -> "other"
      )

      when(mockAddressLookupService.buildAddressLookupUrl(ArgumentMatchers.any[String](), ArgumentMatchers.any[Call]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("testUrl"))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
      }
    }

    "show an error page when fail saving Correspondence Address" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "prepopAddress13"
      )

      val address = Address(
        "testL1",
        "testL2",
        Some("testL3"),
        Some("testL4"),
        Some("testPostCode"),
        None
      )

      when(mockPrepopService.getAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(address))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthHelpers.submitAuthorisedWithCPAndAudit(testController.submitPAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "savePAYECorrespondenceAddress" should {

    implicit val hc = HeaderCarrier()

    "return a DownStreamOutcome SUCCESS" in new Setup {
      val expected =
        Some(Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        ))

      when(mockAddressLookupService.getAddress(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[_]]()))
        .thenReturn(Future.successful(expected))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(expected.get))

      AuthHelpers.showAuthorisedWithCP(testController.savePAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
      }
    }

    "return a DownstreamOutcome FAILURE" in new Setup {
      val expected =
        Some(Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        ))

      when(mockAddressLookupService.getAddress(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[_]]()))
        .thenReturn(Future.successful(expected))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected.get))

      AuthHelpers.showAuthorisedWithCP(testController.savePAYECorrespondenceAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
