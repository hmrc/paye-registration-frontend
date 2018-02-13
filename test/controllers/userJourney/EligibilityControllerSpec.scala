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
import models.view.{CompanyEligibility, DirectorEligibility, Eligibility}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.EligibilityService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val mockEligibilityService = mock[EligibilityService]

  class Setup {
    val controller = new EligibilityController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign


      override val incorpInfoService = mockIncorpInfoService
      override val companyDetailsService = mockCompanyDetailsService
      override val s4LService = mockS4LService
      override val eligibilityService = mockEligibilityService
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"
      override val keystoreConnector = mockKeystoreConnector

      override val authConnector = mockAuthConnector

      override def messagesApi = mockMessagesApi
    }
  }

  val regId = "12345"
  val fakeRequest = FakeRequest("GET", "/")

  val validEligibilityModel = Eligibility(Some(CompanyEligibility(false)), Some(DirectorEligibility(false)))
  val validEmptyModel = Eligibility(None, None)


  "calling the companyEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.companyEligibility, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEligibilityModel))

      AuthHelpers.showAuthorisedWithCP(controller.companyEligibility, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEmptyModel))

      AuthHelpers.showAuthorisedWithCP(controller.companyEligibility, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
      }
    }
  }

  "calling the submitCompanyEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.submitCompanyEligibility, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "return 400 for an invalid answer" in new Setup {
      AuthHelpers.submitAuthorisedWithCP(controller.submitCompanyEligibility(), Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) mustBe Status.BAD_REQUEST
      }
    }

    "redirect to the Ineligibility page when a user enters YES answer" in new Setup {
      when(mockEligibilityService.saveCompanyEligibility(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(controller.submitCompanyEligibility(), Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "true"
      )) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/you-cant-register-online")
      }
    }

    "redirect to the Director Eligibility page when a user enters NO answer" in new Setup {
      when(mockEligibilityService.saveCompanyEligibility(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(controller.submitCompanyEligibility(), Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "false"
      )) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/pay-non-cash-incentive-awards")
      }
    }
  }

  "calling the directorEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.directorEligibility, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEligibilityModel))

      AuthHelpers.showAuthorisedWithCP(controller.directorEligibility, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEmptyModel))

      AuthHelpers.showAuthorisedWithCP(controller.directorEligibility, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
      }
    }
  }

  "calling the submitDirectorEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.submitDirectorEligibility, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "return 400 for an invalid answer" in new Setup {
      AuthHelpers.submitAuthorisedWithCP(controller.submitDirectorEligibility, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) mustBe Status.BAD_REQUEST
      }
    }

    "redirect to the Ineligibility page when a user enters YES answer" in new Setup {
      when(mockEligibilityService.saveDirectorEligibility(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(controller.submitDirectorEligibility, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "true"
      )) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/you-cant-register-online")
      }
    }

    "redirect to the Director Eligibility page when a user enters NO answer" in new Setup {
      when(mockEligibilityService.saveDirectorEligibility(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(controller.submitDirectorEligibility, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "false"
      )) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/use-subcontractors-construction-industry")
      }
    }
  }

  "calling the ineligible action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.ineligible, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "return 200 for an authorised user" in new Setup {

      AuthHelpers.showAuthorisedWithCP(controller.ineligible, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe Status.OK
      }
    }
  }

  "calling the feedback action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.questionnaire, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
      }
    }

    "return 303 and redirect to Company Registration for an authorised user" in new Setup {

      AuthHelpers.showAuthorisedWithCP(controller.questionnaire, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe Status.SEE_OTHER
          redirectLocation(response) mustBe Some("testUrl/testUri/questionnaire")
      }
    }
  }

}
