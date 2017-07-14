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

package controllers.userJourney

import audit.{CorrespondenceAddressAuditEvent, CorrespondenceAddressAuditEventDetail}
import builders.AuthBuilder
import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import fixtures.{PAYERegistrationFixture, S4LFixture}
import models.{Address, DigitalContactDetails}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import models.view.PAYEContactDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{Call, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AddressLookupService, CompanyDetailsService, PAYEContactService, PrepopulationService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import services._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PAYEContactControllerSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture {
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockPAYEContactService = mock[PAYEContactService]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]
  val mockAddressLookupService = mock[AddressLookupService]
  val mockMessagesApi = mock[MessagesApi]
  val mockPrepopService = mock[PrepopulationService]

  val regId = "12345"

  class Setup {
    val testController = new PAYEContactCtrl {
      override val companyDetailsService = mockCompanyDetailsService
      override val payeContactService = mockPAYEContactService
      override val addressLookupService = mockAddressLookupService
      override val keystoreConnector = mockKeystoreConnector
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val authConnector = mockAuthConnector
      override val payeRegistrationConnector = mockPayeRegistrationConnector
      override val prepopService = mockPrepopService

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result], payeRegistrationSubmitted: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          regId,
          CompanyRegistrationProfile("held", "txId"),
          "ENG",
          payeRegistrationSubmitted = false
        ))
      }
    }
  }

  "payeContactDetails" should {
    "return a SEE_OTHER if user is not authorised" in new Setup {
      val result = testController.payeContactDetails()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return an OK with data from registration" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(validPAYEContactView)

      AuthBuilder.showWithAuthorisedUser(testController.payeContactDetails, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }

    "return an OK with data from prepopulation" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(emptyPAYEContactView)

      AuthBuilder.showWithAuthorisedUser(testController.payeContactDetails, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }

    "return an OK without data" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(emptyPAYEContactView)

      when(mockPrepopService.getPAYEContactDetails(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(None)

      AuthBuilder.showWithAuthorisedUser(testController.payeContactDetails, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }
  }

  "submitPAYEContactDetails" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      val result = testController.submitPAYEContactDetails()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return a BAD_REQUEST if there is problem with the submitted form, no name" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> ""
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form, no contact details" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "teeeeeeest"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "tata",
        "digitalContact.contactEmail" -> "tata@test.com"
      )

      when(mockPAYEContactService.submitPayeContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return a SEE_OTHER and redirect to the Correspondence Address page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "tata",
        "digitalContact.contactEmail" -> "tata@test.com"
      )

      when(mockPAYEContactService.submitPayeContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveContactDetails(ArgumentMatchers.eq(regId), ArgumentMatchers.any[PAYEContactDetails]())(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/register-for-paye/where-to-send-post")
      }
    }
  }

  "payeCorrespondenceAddress" should {
    "return an OK" in new Setup {
      val addressMap = Map(
        "ro" -> validCompanyDetailsViewModel.roAddress,
        "correspondence" -> validCompanyDetailsViewModel.ppobAddress.get
      )
      mockFetchCurrentProfile()

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validPAYEContactView))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPrepopService.getPrePopAddresses(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Map.empty[Int, Address]))

      when(mockPAYEContactService.getCorrespondenceAddresses(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(addressMap)

      AuthBuilder.showWithAuthorisedUser(testController.payeCorrespondenceAddress, mockAuthConnector) { result =>
        status(result) shouldBe OK
      }
    }
  }

  "submitPAYECorrespondenceAddress" should {
    implicit val hc = HeaderCarrier()

    val auditEvent = new CorrespondenceAddressAuditEvent(CorrespondenceAddressAuditEventDetail(
      "testExternalUserId",
      "testAuthProviderId",
      "testRegID",
      "testAddressUsed"
    ))

    "return a BAD_REQUEST" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> ""
      )

      val addressMap = Map(
        "ro" -> validCompanyDetailsViewModel.roAddress,
        "correspondence" -> validCompanyDetailsViewModel.ppobAddress.get
      )

      when(mockPAYEContactService.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validPAYEContactView))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.getCorrespondenceAddresses(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(addressMap)

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to summary if correspondence is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "correspondenceAddress"
      )

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/register-for-paye/check-and-confirm-your-answers")
      }
    }

    "redirect to summary if ro is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "roAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPAYEContactService.auditCorrespondenceAddress(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[AuthContext](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(auditEvent))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/register-for-paye/check-and-confirm-your-answers")
      }
    }

    "redirect to summary if ppob is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "ppobAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPAYEContactService.auditCorrespondenceAddress(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[AuthContext](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(auditEvent))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/register-for-paye/check-and-confirm-your-answers")
      }
    }

    "return a 500 in case of DownstreamOutcome FAILURE when saving with a missing PPOB Address" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "ppobAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel.copy(ppobAddress = None)))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return a 500 in case of DownstreamOutcome FAILURE" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "roAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to address lookup frontend if other is chosen" in new Setup {
      val request = FakeRequest("GET", "/testuri?id=123456789").withFormUrlEncodedBody(
        "chosenAddress" -> "other"
      )

      when(mockAddressLookupService.buildAddressLookupUrl(ArgumentMatchers.any[String](), ArgumentMatchers.any[Call]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("testUrl"))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
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

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYECorrespondenceAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
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

      AuthBuilder.showWithAuthorisedUser(testController.savePAYECorrespondenceAddress, mockAuthConnector) { result =>
        status(result) shouldBe SEE_OTHER
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

      AuthBuilder.showWithAuthorisedUser(testController.savePAYECorrespondenceAddress, mockAuthConnector) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
