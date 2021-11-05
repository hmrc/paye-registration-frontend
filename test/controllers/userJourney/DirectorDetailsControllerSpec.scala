/*
 * Copyright 2021 HM Revenue & Customs
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
import models.api.{Director, Name}
import models.view.{Directors, Ninos, UserEnteredNino}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import services.DirectorDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.directorDetails
import views.html.pages.error.restart

import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}
import scala.concurrent.Future

class DirectorDetailsControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val mockDirectorDetailService = mock[DirectorDetailsService]

  val fakeRequest = FakeRequest()

  lazy val mockMcc = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockDirectorDetailsPage = app.injector.instanceOf[directorDetails]
  lazy val mockRestart = app.injector.instanceOf[restart]

  class Setup {
    val testController = new DirectorDetailsController(
      mockDirectorDetailService,
      mockKeystoreConnector,
      mockS4LService,
      mockCompanyDetailsService,
      mockIncorpInfoService,
      mockAuthConnector,
      mockIncorpInfoConnector,
      mockPayeRegService,
      mockMcc,
      mockDirectorDetailsPage,
      mockRestart
    )(mockAppConfig,
      globalExecutionContext)
  }

  val testDirectors =
    Directors(
      Map(
        "0" -> Director(
          Name(
            Some("testName"),
            Some("testName"),
            "testName",
            Some("testName")
          ),
          Some("testNino")
        )
      )
    )

  val userNinos = Ninos(
    List(
      UserEnteredNino("0", Some("testNino"))
    )
  )
  val directorMap = Map("0" -> "testName testName")

  "directorDetails" should {
    "return a SEE_OTHER if user is not authorised" in new Setup {
      AuthHelpers.showUnauthorised(testController.directorDetails, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return an OK" in new Setup {
      when(mockDirectorDetailService.getDirectorDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(testDirectors))

      when(mockDirectorDetailService.createDirectorNinos(ArgumentMatchers.any()))
        .thenReturn(userNinos)

      when(mockDirectorDetailService.createDisplayNamesMap(ArgumentMatchers.any()))
        .thenReturn(directorMap)

      AuthHelpers.showAuthorisedWithCP(testController.directorDetails, Fixtures.validCurrentProfile, FakeRequest()) {
        (result: Future[Result]) =>
          status(result) mustBe OK
      }
    }
  }

  "submitDirectorDetails" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      AuthHelpers.showUnauthorised(testController.submitDirectorDetails, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "nino[0]" -> ""
      )
      when(mockDirectorDetailService.getDirectorDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(testDirectors))

      when(mockDirectorDetailService.createDisplayNamesMap(ArgumentMatchers.any()))
        .thenReturn(directorMap)

      AuthHelpers.submitAuthorisedWithCP(testController.submitDirectorDetails, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "return a SEE_OTHER and redirect to the PAYE Contact page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody()
      when(mockDirectorDetailService.submitNinos(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(testController.submitDirectorDetails, Fixtures.validCurrentProfile, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/who-should-we-contact")
      }
    }
  }
}