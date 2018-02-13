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

import enums.{DownstreamOutcome, RegistrationDeletion}
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.{BusinessProfile, CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

class PayeStartControllerSpec extends PayeComponentSpec with PayeFakedApp {

  class Setup {
    val controller = new PayeStartController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService              = mockIncorpInfoService
      override val companyDetailsService          = mockCompanyDetailsService
      override val s4LService                     = mockS4LService
      override val authConnector                  = mockAuthConnector
      override val currentProfileService          = mockCurrentProfileService
      override val payeRegistrationService        = mockPayeRegService
      implicit val messagesApi: MessagesApi       = mockMessagesApi
      override val compRegFEURL: String           = "testUrl"
      override val compRegFEURI: String           = "/testUri"
      override val keystoreConnector              = mockKeystoreConnector
      override val businessRegistrationConnector  = mockBusinessRegistrationConnector
      override val companyRegistrationConnector   = mockCompRegConnector
    }
  }

  val fakeRequest = FakeRequest()

  def validCurrentProfile(status: String) = CurrentProfile("testRegId", CompanyRegistrationProfile(status, "txId"), "en", false)

  "Calling the startPaye action" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "force the user to create a new account" in new Setup {
      AuthHelpers.showAuthorisedNotOrg(controller.startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/post-sign-in")
      }
    }

    "show an Error page for an authorised user without a registration ID" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception))

      AuthHelpers.showAuthorisedOrg(controller.startPaye, fakeRequest) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID and CoHo Company Details, with an error response from the microservice" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.validCurrentProfile.get))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(DownstreamOutcome.Failure))

      AuthHelpers.showAuthorisedOrg(controller.startPaye, FakeRequest()) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the start page for an authorised user with a registration ID and CoHo Company Details, with PAYE Footprint correctly asserted" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held")))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller.startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/register-as-employer")
      }
    }

    "redirect to the CT start page for a user with no CT Footprint found" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("404")))

      AuthHelpers.showAuthorisedOrg(controller.startPaye, fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("testUrl/testUri/register")
      }
    }

    "redirect the user to the start of Incorporation and Corporation Tax if their Company Registration document has a status of 'draft'" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("draft")))

      AuthHelpers.showAuthorisedOrg(controller.startPaye, fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/register")
      }
    }
  }

  "restartPaye" should {
    "redirect to start" when {
      "the users document is deleted and are going to start their application again" in new Setup {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(validCurrentProfile("rejected"))))

        when(mockPayeRegService.deletePayeRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.success))

        AuthHelpers.showAuthorised(controller.restartPaye, fakeRequest) {
          result =>
            status(result)           mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(s"/register-for-paye")
        }
      }

      "the users document is deleted and are going to start their application again but there wasn't a current profile in session" in new Setup {
        val testBusinessProfile = BusinessProfile(
          "testRegId",
          "ENG"
        )

        val testCompanyProfile = CompanyRegistrationProfile(
          "rejected",
          "testTxId"
        )

        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(testBusinessProfile))

        when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(testCompanyProfile))

        when(mockPayeRegService.deletePayeRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.success))

        AuthHelpers.showAuthorised(controller.restartPaye, fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(s"/register-for-paye")
        }
      }
    }

    "redirect to dashboard" when {
      "the users document has not been deleted as it was not rejected" in new Setup {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(validCurrentProfile("rejected"))))

        when(mockPayeRegService.deletePayeRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.invalidStatus))

        AuthHelpers.showAuthorised(controller.restartPaye, fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.userJourney.routes.DashboardController.dashboard().url)
        }
      }
    }
  }
}
