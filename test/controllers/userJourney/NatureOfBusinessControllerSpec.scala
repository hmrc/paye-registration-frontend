/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import services.NatureOfBusinessService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.error.restart
import views.html.pages.natureOfBusiness

import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}
import scala.concurrent.Future

class NatureOfBusinessControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val mockNatureOfBusinessService: NatureOfBusinessService = mock[NatureOfBusinessService]

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockRestart: restart = app.injector.instanceOf[restart]
  lazy val mockNatureOfBusinessPage: natureOfBusiness = app.injector.instanceOf[natureOfBusiness]

  class Setup {
    val testController: NatureOfBusinessController = new NatureOfBusinessController(
      mockNatureOfBusinessService,
      mockKeystoreConnector,
      mockS4LService,
      mockCompanyDetailsService,
      mockIncorpInfoService,
      mockAuthConnector,
      mockIncorpInfoConnector,
      mockPayeRegService,
      mockMcc,
      mockNatureOfBusinessPage,
      mockRestart
    )(injAppConfig,
      globalExecutionContext)
  }

  val testNOB = NatureOfBusiness(natureOfBusiness = "laundring")

  "natureOfBusiness" should {
    "return a SEE_OTHER if user is not authorised" in new Setup {
      AuthHelpers.showUnauthorised(testController.natureOfBusiness, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return an OK when there is a nature of business in the microservice" in new Setup {
      when(mockNatureOfBusinessService.getNatureOfBusiness(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(testNOB)))

      AuthHelpers.showAuthorisedWithCP(testController.natureOfBusiness, Fixtures.validCurrentProfile, fakeRequest()) {
        (result: Future[Result]) =>
          status(result) mustBe OK
      }
    }

    "return an OK when there is no nature of business in the microservice" in new Setup {
      when(mockNatureOfBusinessService.getNatureOfBusiness(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showAuthorisedWithCP(testController.natureOfBusiness, Fixtures.validCurrentProfile, fakeRequest()) {
        (result: Future[Result]) =>
          status(result) mustBe OK
      }
    }
  }

  "submitNatureOfBusiness" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      AuthHelpers.submitUnauthorised(testController.natureOfBusiness, fakeRequest("POST")) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return a BAD_REQUEST if the submitted form is empty" in new Setup {
      val request = fakeRequest("POST").withFormUrlEncodedBody(
        "description" -> ""
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form" in new Setup {
      val request = fakeRequest("POST").withFormUrlEncodedBody(
        "description" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      when(mockNatureOfBusinessService.saveNatureOfBusiness(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(DownstreamOutcome.Failure))
      AuthHelpers.submitAuthorisedWithCP(testController.submitNatureOfBusiness(), Fixtures.validCurrentProfile, fakeRequest("POST").withFormUrlEncodedBody(
        "description" -> "testing"
      )) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return a SEE_OTHER and redirect to the directors page" in new Setup {
      val request = fakeRequest("POST").withFormUrlEncodedBody(
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