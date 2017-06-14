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

import builders.AuthBuilder
import connectors._
import enums.PAYEStatus
import fixtures.PAYERegistrationFixture
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import services.{SubmissionService, SummaryService}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class SummaryControllerSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockSummaryService = mock[SummaryService]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]
  val mockSubmissionService = mock[SubmissionService]

  class Setup {
    val controller = new SummaryCtrl {
      override val summaryService = mockSummaryService
      override val authConnector = mockAuthConnector
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegistrationConnector = mockPayeRegistrationConnector
      override val submissionService = mockSubmissionService
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result], payeRegistrationSubmitted: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          "12345",
          "Director",
          CompanyRegistrationProfile("held", "txId"),
          "ENG",
          payeRegistrationSubmitted = false
        ))
      }
    }
  }

  "Calling summary to show the summary page" should {
    "show the summary page when a valid model is returned from the microservice and the reg doc status is draft" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYERegistrationAPI))

      when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validSummaryView))

      AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body().getElementById("pageHeading").text() shouldBe "Check and confirm your answers"
          result.body.getElementById("tradingNameAnswer").text() shouldBe "tstTrade"
      }
    }

    "return an Internal Server Error response when no valid model is returned from the microservice" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYERegistrationAPI))

      when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

      AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return a see other" when {
      "the reg document status is held" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(validPAYERegistrationAPI.copy(status = PAYEStatus.held)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
          (response: Future[Result]) =>
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/register-for-paye/application-submitted")
        }
      }

      "the reg document status is submitted" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(validPAYERegistrationAPI.copy(status = PAYEStatus.submitted)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
          (response: Future[Result]) =>
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/register-for-paye/application-submitted")
        }
      }

      "the reg document status is invalid" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(validPAYERegistrationAPI.copy(status = PAYEStatus.invalid)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
          (response: Future[Result]) =>
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/register-for-paye/ineligible-for-paye")
        }
      }

      "the reg document status is rejected" in new Setup {
        when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(validPAYERegistrationAPI.copy(status = PAYEStatus.rejected)))

        when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

        AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
          (response: Future[Result]) =>
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/register-for-paye/ineligible-for-paye")
        }
      }
    }
  }

  "Calling submitRegistration" should {
    "show the confirmation page" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(Success))

      AuthBuilder.showWithAuthorisedUser(controller.submitRegistration, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe "/register-for-paye/application-submitted"
      }
    }
    "show the dashboard" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(Cancelled))

      AuthBuilder.showWithAuthorisedUser(controller.submitRegistration, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe "/register-for-paye/business-registration-overview"
      }
    }
    "show the retry page" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(TimedOut))

      AuthBuilder.showWithAuthorisedUser(controller.submitRegistration, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    "show the deskpro page" in new Setup {
      when(mockPayeRegistrationConnector.getRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYERegistrationAPI))

      when(mockSubmissionService.submitRegistration(ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any())).thenReturn(Future.successful(Failed))

      AuthBuilder.showWithAuthorisedUser(controller.submitRegistration, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe "/register-for-paye/something-went-wrong"
      }
    }
  }

}
