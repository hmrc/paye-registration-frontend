/*
 * Copyright 2023 HM Revenue & Customs
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

import ch.qos.logback.classic.Level
import common.exceptions.DownstreamExceptions.{CurrentProfileNotFoundException, UnexpectedException}
import enums.{DownstreamOutcome, RegistrationDeletion}
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.{BusinessProfile, CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.NotFoundException
import utils.LogCapturingHelper
import views.html.pages.error.restart

import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}
import scala.concurrent.Future

class PayeStartControllerSpec extends PayeComponentSpec with PayeFakedApp with LogCapturingHelper {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockRestart: restart = app.injector.instanceOf[restart]

  class Setup {
    def controller() = new PayeStartController(
      currentProfileService = mockCurrentProfileService,
      payeRegistrationService = mockPayeRegService,
      keystoreConnector = mockKeystoreConnector,
      authConnector = mockAuthConnector,
      s4LService = mockS4LService,
      companyDetailsService = mockCompanyDetailsService,
      incorpInfoService = mockIncorpInfoService,
      businessRegistrationConnector = mockBusinessRegistrationConnector,
      companyRegistrationConnector = mockCompRegConnector,
      featureSwitches = mockFeatureSwitch,
      incorporationInformationConnector = mockIncorpInfoConnector,
      mcc = mockMcc,
      restart = mockRestart,
      languageUtils = mockLanguageUtils
    )(injAppConfig,
      globalExecutionContext)
  }

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def validCurrentProfile(status: String, ackRefStatus: Option[String] = None): CurrentProfile =
    CurrentProfile("testRegId", CompanyRegistrationProfile(status, "txId", ackRefStatus), "en", payeRegistrationSubmitted = false, None)

  "steppingStone" should {
    "redirect to PREFE" in new Setup {
      AuthHelpers.showUnauthorised(controller().steppingStone(), fakeRequest) { resp =>
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("http://localhost:9877/eligibility-for-paye")
      }
    }
  }

  "Calling the startPaye action" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "force the user to create a new account" in new Setup {
      AuthHelpers.showAuthorisedNotOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }
    }

    "show an Error page for an authorised user without a registration ID" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID and CoHo Company Details, with an error response from the microservice" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCurrentProfile.get))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), FakeRequest()) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the paid employees page for an authorised user with a registration ID and CoHo Company Details, with PAYE Footprint correctly asserted" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("submitted")))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.paidEmployees.url)
      }
    }

    "redirect to the paid employees page for an authorised user with valid details, with PAYE Footprint correctly asserted, with CT accepted" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("acknowledged", Some("04"))))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.paidEmployees.url)
      }
    }

    "redirect to the paid employees page for an authorised user with valid details, with CT submitted and with incorporation paid" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(
          CurrentProfile("testRegId", CompanyRegistrationProfile("held", "txId", None, Some("paid")), "en", payeRegistrationSubmitted = false, None)
        ))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.paidEmployees.url)
      }
    }

    "redirect to Company Registration for an authorised user with valid details, with CT submitted but with incorporation unpaid" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(
          CurrentProfile("testRegId", CompanyRegistrationProfile("held", "txId", None, None), "en", payeRegistrationSubmitted = false, None)
        ))

      when(mockPayeRegService.assertRegistrationFootprint(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/post-sign-in")
      }
    }
    
    "redirect to OTRS when a NOT_FOUND exception is thrown" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("404")))

      withCaptureOfLoggingFrom(controller().logger) { logs =>

        AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(injAppConfig.otrsUrl)
            logs.containsMsg(Level.WARN, "[PayeStartController][checkAndStoreCurrentProfile] NotFoundException redirecting to OTRS")
        }
      }
    }

    "redirect to OTRS when a CurrentProfileNotFoundException is thrown" in new Setup {

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new CurrentProfileNotFoundException))

      withCaptureOfLoggingFrom(controller().logger) { logs =>
        AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(injAppConfig.otrsUrl)
            logs.containsMsg(Level.WARN, "[PayeStartController][checkAndStoreCurrentProfile] no company profile found. Redirecting to OTRS")
        }
      }
    }

    "redirect to OTRS for a user with no CT confirmation references" in new Setup {

      import common.exceptions.DownstreamExceptions.ConfirmationRefsNotFoundException

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ConfirmationRefsNotFoundException))

      withCaptureOfLoggingFrom(controller().logger) { logs =>
        AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(injAppConfig.otrsUrl)
            logs.containsMsg(Level.WARN, "[PayeStartController][checkAndStoreCurrentProfile] ConfirmationRefsNotFoundException redirecting to OTRS")
        }
      }
    }

    "redirect to OTRS when an unexpected exception is thrown" in new Setup {

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new UnexpectedException("oh no")))

      withCaptureOfLoggingFrom(controller().logger) { logs =>
        AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
          result =>
            status(result) mustBe Status.INTERNAL_SERVER_ERROR
            logs.containsMsg(Level.ERROR, "[checkAndStoreCurrentProfile] failed to checkAndStoreCurrentProfile. Error: common.exceptions.DownstreamExceptions$UnexpectedException: oh no")
        }
      }
    }

    "redirect the user to OTRS if their Company Registration document has a status of 'draft'" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("draft")))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }
    }

    "redirect the user to post sign in if their CT is rejected" in new Setup {
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("acknowledged", Some("06"))))

      AuthHelpers.showAuthorisedOrg(controller().startPaye(), fakeRequest) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.SignInOutController.postSignIn.toString)
      }
    }
  }

  "restartPaye" should {
    "redirect to start" when {
      "the users document is deleted and are going to start their application again" in new Setup {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(validCurrentProfile("rejected"))))

        when(mockPayeRegService.deleteRejectedRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.success))

        AuthHelpers.showAuthorised(controller().restartPaye, fakeRequest) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(s"/register-for-paye/start-pay-as-you-earn")
        }
      }

      "the users document is deleted and are going to start their application again but there wasn't a current profile in session" in new Setup {
        val testBusinessProfile: BusinessProfile = BusinessProfile(
          "testRegId",
          "ENG"
        )

        val testCompanyProfile: CompanyRegistrationProfile = CompanyRegistrationProfile(
          "rejected",
          "testTxId"
        )

        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(testBusinessProfile))

        when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(testCompanyProfile))

        when(mockPayeRegService.deleteRejectedRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.success))

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

        when(mockPayeRegService.deleteRejectedRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.invalidStatus))

        AuthHelpers.showAuthorised(controller().restartPaye, fakeRequest) {
          result =>
            status(result) mustBe Status.SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.userJourney.routes.DashboardController.dashboard.url)
        }
      }
    }
  }
}
