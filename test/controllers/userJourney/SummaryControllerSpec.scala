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

import connectors._
import enums.PAYEStatus
import helpers.auth.AuthHelpers
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.CurrentProfile
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.{SubmissionService, SummaryService}

import scala.concurrent.Future

class SummaryControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val mockSummaryService    = mock[SummaryService]
  val mockSubmissionService = mock[SubmissionService]

  class Setup extends AuthHelpers {
    override val authConnector = mockAuthConnector
    override val keystoreConnector = mockKeystoreConnector

    val controller = new SummaryController {
      override val redirectToLogin            = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign         = MockAuthRedirects.redirectToPostSign
      override val emailService               = mockEmailService
      override val incorpInfoService          = mockIncorpInfoService
      override val companyDetailsService      = mockCompanyDetailsService
      override val s4LService                 = mockS4LService
      override val summaryService             = mockSummaryService
      override val authConnector              = mockAuthConnector
      override val keystoreConnector          = mockKeystoreConnector
      override val payeRegistrationConnector  = mockPayeRegistrationConnector
      override val submissionService          = mockSubmissionService
      implicit val messagesApi: MessagesApi   = mockMessagesApi
    }
  }

  "Calling summary to show the summary page" should {
    "show the summary page when a valid model is returned from the microservice and the reg doc status is draft" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI))

      when(mockEmailService.primeEmailData(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.blankCacheMap))

      when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validSummaryView))

      showAuthorisedWithCP(controller.summary, Fixtures.validCurrentProfile, FakeRequest()) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
          val result = Jsoup.parse(contentAsString(response))
          result.body().getElementById("pageHeading").text() mustBe "Check and confirm your answers"
          result.body.getElementById("tradingNameAnswer").text() mustBe "tstTrade"
      }
    }

    "return an Internal Server Error response when no valid model is returned from the microservice" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI))

      when(mockEmailService.primeEmailData(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.blankCacheMap))

      when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

      intercept[Exception](showAuthorisedWithCP(controller.summary, Fixtures.validCurrentProfile, FakeRequest()) {
        (response: Future[Result]) =>
          status(response) mustBe Status.INTERNAL_SERVER_ERROR
      })
    }

    "return a see other" when {
      "the reg document status is held" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI.copy(status = PAYEStatus.held)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        showAuthorisedWithCP(controller.summary, Fixtures.validCurrentProfile, FakeRequest()) {
          (response: Future[Result]) =>
            status(response) mustBe Status.SEE_OTHER
            redirectLocation(response) mustBe Some("/register-for-paye/application-submitted")
        }
      }

      "the reg document status is submitted" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI.copy(status = PAYEStatus.submitted)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        showAuthorisedWithCP(controller.summary, Fixtures.validCurrentProfile, FakeRequest()) {
          (response: Future[Result]) =>
            status(response) mustBe Status.SEE_OTHER
            redirectLocation(response) mustBe Some("/register-for-paye/application-submitted")
        }
      }

      "the reg document status is invalid" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI.copy(status = PAYEStatus.invalid)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        showAuthorisedWithCP(controller.summary, Fixtures.validCurrentProfile, FakeRequest()) {
          (response: Future[Result]) =>
            status(response) mustBe Status.SEE_OTHER
            redirectLocation(response) mustBe Some("/register-for-paye/ineligible-for-paye")
        }
      }

      "the reg document status is rejected" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI.copy(status = PAYEStatus.rejected)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        showAuthorisedWithCP(controller.summary, Fixtures.validCurrentProfile, FakeRequest()) {
          (response: Future[Result]) =>
            status(response) mustBe Status.SEE_OTHER
            redirectLocation(response) mustBe Some("/register-for-paye/ineligible-for-paye")
        }
      }
    }
  }

  "Calling submitRegistration" should {
    "show the confirmation page" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(Success))

      showAuthorisedWithCP(controller.submitRegistration, Fixtures.validCurrentProfile, FakeRequest()) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/application-submitted")
      }
    }
    "show the dashboard" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(Cancelled))

      showAuthorisedWithCP(controller.submitRegistration, Fixtures.validCurrentProfile, FakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/business-registration-overview")
      }
    }
    "show the retry page" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(TimedOut))

      showAuthorisedWithCP(controller.submitRegistration, Fixtures.validCurrentProfile, FakeRequest()) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "show the deskpro page" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(Failed))

      showAuthorisedWithCP(controller.submitRegistration, Fixtures.validCurrentProfile, FakeRequest()) {
        (result: Future[Result]) =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/something-went-wrong")
      }
    }
  }

}
