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
import models.view.NatureOfBusiness
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.NatureOfBusinessService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class NatureOfBusinessControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val mockNatureOfBusinessService = mock[NatureOfBusinessService]

  class Setup {
    val testController = new NatureOfBusinessController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService = mockIncorpInfoService
      override val companyDetailsService = mockCompanyDetailsService
      override val s4LService = mockS4LService
      override val authConnector = mockAuthConnector
      override val natureOfBusinessService = mockNatureOfBusinessService
      override val keystoreConnector = mockKeystoreConnector
      implicit val messagesApi: MessagesApi = mockMessagesApi
    }
  }

  val testNOB = NatureOfBusiness(natureOfBusiness = "laundring")

  val request = FakeRequest()

  "natureOfBusiness" should {
    "return a SEE_OTHER if user is not authorised" in new Setup {
      AuthHelpers.showUnauthorised(testController.natureOfBusiness, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return an OK when there is a nature of business in the microservice" in new Setup {
      when(mockNatureOfBusinessService.getNatureOfBusiness(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(testNOB)))

      AuthHelpers.showAuthorisedWithCP(testController.natureOfBusiness, Fixtures.validCurrentProfile, request) {
        (result: Future[Result])  =>
          status(result) mustBe OK
      }
    }

    "return an OK when there is no nature of business in the microservice" in new Setup {
      when(mockNatureOfBusinessService.getNatureOfBusiness(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showAuthorisedWithCP(testController.natureOfBusiness, Fixtures.validCurrentProfile, request) {
        (result: Future[Result])  =>
          status(result) mustBe OK
      }
    }
  }

  "submitNatureOfBusiness" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      AuthHelpers.submitUnauthorised(testController.natureOfBusiness, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return a BAD_REQUEST if the submitted form is empty" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "description" -> ""
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "description" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      when(mockNatureOfBusinessService.saveNatureOfBusiness(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(DownstreamOutcome.Failure))
      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness(), Fixtures.validCurrentProfile, FakeRequest().withFormUrlEncodedBody(
        "description" -> "testing"
      )) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return a SEE_OTHER and redirect to the directors page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "description" -> "computing"
      )
      when(mockNatureOfBusinessService.saveNatureOfBusiness(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/director-national-insurance-number")
      }
    }
  }
}
