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
import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import fixtures.S4LFixture
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import models.view.NatureOfBusiness
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CompanyDetailsSrv, NatureOfBusinessSrv}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class NatureOfBusinessControllerSpec extends PAYERegSpec with S4LFixture {

  val mockNatureOfBusinessService = mock[NatureOfBusinessSrv]
  val mockCompanyDetailsService = mock[CompanyDetailsSrv]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]

  class Setup {
    val testController = new NatureOfBusinessCtrl {
      override val authConnector = mockAuthConnector
      override val natureOfBusinessService = mockNatureOfBusinessService
      override val companyDetailsService = mockCompanyDetailsService
      override val keystoreConnector = mockKeystoreConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val payeRegistrationConnector = mockPayeRegistrationConnector

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result], payeRegistrationSubmitted: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          "12345",
          CompanyRegistrationProfile("held", "txId"),
          "ENG",
          payeRegistrationSubmitted = false
        ))
      }
    }
  }

  val testNOB = NatureOfBusiness(natureOfBusiness = "laundring")

  "natureOfBusiness" should {
    "return a SEE_OTHER if user is not authorised" in new Setup {
      val result = testController.natureOfBusiness()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return an OK when there is a nature of business in the microservice" in new Setup {
      when(mockNatureOfBusinessService.getNatureOfBusiness(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(testNOB)))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(validCompanyDetailsViewModel)

      AuthBuilder.showWithAuthorisedUser(testController.natureOfBusiness, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }

    "return an OK when there is no nature of business in the microservice" in new Setup {
      when(mockNatureOfBusinessService.getNatureOfBusiness(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(validCompanyDetailsViewModel)

      AuthBuilder.showWithAuthorisedUser(testController.natureOfBusiness, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }
  }

  "submitNatureOfBusiness" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      val result = testController.submitNatureOfBusiness()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return a BAD_REQUEST if the submitted form is empty" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "description" -> ""
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(validCompanyDetailsViewModel)

      AuthBuilder.submitWithAuthorisedUser(testController.submitNatureOfBusiness, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "description" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(validCompanyDetailsViewModel)

      AuthBuilder.submitWithAuthorisedUser(testController.submitNatureOfBusiness, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      when(mockNatureOfBusinessService.saveNatureOfBusiness(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(DownstreamOutcome.Failure))
      AuthBuilder.submitWithAuthorisedUser(testController.submitNatureOfBusiness(), mockAuthConnector, FakeRequest().withFormUrlEncodedBody(
        "description" -> "testing"
      )) {
        result =>
          status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return a SEE_OTHER and redirect to the directors page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "description" -> "computing"
      )
      when(mockNatureOfBusinessService.saveNatureOfBusiness(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(validCompanyDetailsViewModel)

      AuthBuilder.submitWithAuthorisedUser(testController.submitNatureOfBusiness, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/register-for-paye/director-national-insurance-number")
      }
    }
  }
}
