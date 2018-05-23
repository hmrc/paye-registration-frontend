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

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    System.clearProperty("feature.publicBeta")
  }

  class Setup {
    def controller(pbEnabled: Boolean = false, naEnabled: Boolean = false) = new PayeStartController {
      override val publicBetaEnabled       = pbEnabled
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign
      override val newApiEnabled           = naEnabled

      override val payeRegElFEURL          = MockAuthRedirects.payeRegElFEUrl
      override val payeRegElFEURI          = MockAuthRedirects.payeRegElFEUri

      override val incorpInfoService              = mockIncorpInfoService
      override val companyDetailsService          = mockCompanyDetailsService
      override val s4LService                     = mockS4LService
      override val authConnector                  = mockAuthConnector
      override val currentProfileService          = mockCurrentProfileService
      override val payeRegistrationService        = mockPayeRegService
      implicit val messagesApi: MessagesApi       = mockMessagesApi
      override val keystoreConnector              = mockKeystoreConnector
      override val businessRegistrationConnector  = mockBusinessRegistrationConnector
      override val companyRegistrationConnector   = mockCompRegConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
    }
  }

  val fakeRequest = FakeRequest()

  def validCurrentProfile(status: String, ackRefStatus : Option[String] = None) =
    CurrentProfile("testRegId", CompanyRegistrationProfile(status, "txId", ackRefStatus), "en", false, None)

  "steppingStone" should {
    "redirect to PREFE" when {
      "public beta is enabled" in new Setup {
        System.setProperty("feature.publicBeta", "true")

        AuthHelpers.showUnauthorised(controller(pbEnabled = true).steppingStone(), fakeRequest) { resp =>
          status(resp)           mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some("/prefe/test/")
        }
      }
    }

    "redirect to the startPaye route" when {
      "public beta is disabled" in new Setup {
        System.setProperty("feature.publicBeta", "false")

        AuthHelpers.showUnauthorised(controller().steppingStone(), fakeRequest) { resp =>
          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some("/register-for-paye/start-pay-as-you-earn")
        }
      }

      "public beta is not defined" in new Setup {
        AuthHelpers.showUnauthorised(controller().steppingStone(), fakeRequest) { resp =>
          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some("/register-for-paye/start-pay-as-you-earn")
        }
      }
    }
  }

  "Calling the startPaye action" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "force the user to create a new account" in new Setup {
      AuthHelpers.showAuthorisedNotOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }
    }

    "show an Error page for an authorised user without a registration ID" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID and CoHo Company Details, with an error response from the microservice" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.validCurrentProfile.get))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(DownstreamOutcome.Failure))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, FakeRequest()) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the start page for an authorised user with a registration ID and CoHo Company Details, with PAYE Footprint correctly asserted" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held")))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/register-as-employer")
      }
    }

    "redirect to the start page for an authorised user with valid details, with PAYE Footprint correctly asserted, with CT accepted" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held", Some("04"))))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/register-as-employer")
      }
    }

    "redirect to OTRS for a user with no CT Footprint found" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("404")))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }
    }

    "redirect to OTRS for a user with no CT confirmation references" in new Setup {
      import common.exceptions.DownstreamExceptions.ConfirmationRefsNotFoundException

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ConfirmationRefsNotFoundException))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }
    }

    "redirect the user to OTRS if their Company Registration document has a status of 'draft'" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("draft")))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }
    }

    "redirect the user to post sign in if their CT is rejected" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("submitted", Some("06"))))

      AuthHelpers.showAuthorisedOrg(controller().startPaye, fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.SignInOutController.postSignIn().toString)
      }
    }
  }

  "restartPaye" should {
    "redirect to start" when {
      "the users document is deleted and are going to start their application again" in new Setup {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(validCurrentProfile("rejected"))))

        when(mockPayeRegService.deleteRejectedRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.success))

        AuthHelpers.showAuthorised(controller().restartPaye, fakeRequest) {
          result =>
            status(result)           mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(s"/register-for-paye/start-pay-as-you-earn")
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

        when(mockPayeRegService.deleteRejectedRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.success))

        AuthHelpers.showAuthorised(controller().restartPaye, fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(s"/register-for-paye/start-pay-as-you-earn")
        }
      }
    }

    "redirect to dashboard" when {
      "the users document has not been deleted as it was not rejected" in new Setup {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(validCurrentProfile("rejected"))))

        when(mockPayeRegService.deleteRejectedRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.invalidStatus))

        AuthHelpers.showAuthorised(controller().restartPaye, fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.userJourney.routes.DashboardController.dashboard().url)
        }
      }
    }
  }
}
