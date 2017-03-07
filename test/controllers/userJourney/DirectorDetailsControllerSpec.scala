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
import enums.DownstreamOutcome
import models.api.{Director, Name}
import models.view.{Directors, Ninos, UserEnteredNino}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DirectorDetailsSrv
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class DirectorDetailsControllerSpec extends PAYERegSpec {

  val mockDirectorDetailService = mock[DirectorDetailsSrv]
  val mockMessagesApi = mock[MessagesApi]

  class Setup {
    val testController = new DirectorDetailsCtrl {
      override val directorDetailsService = mockDirectorDetailService
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val authConnector = mockAuthConnector
    }
  }

  val testDirectors =
    Directors(
      Map(
        "0" -> Director(
          Name(
            Some("testName"),
            Some("testName"),
            Some("testName"),
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
      val result = testController.directorDetails()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return an OK" in new Setup {
      when(mockDirectorDetailService.getDirectorDetails()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(testDirectors))

      when(mockDirectorDetailService.createDirectorNinos(Matchers.any()))
        .thenReturn(userNinos)

      when(mockDirectorDetailService.createDisplayNamesMap(Matchers.any()))
        .thenReturn(directorMap)

      AuthBuilder.showWithAuthorisedUser(testController.directorDetails, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }
  }

  "submitDirectorDetails" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      val result = testController.submitDirectorDetails()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return a BAD_REQUEST if there is problem with the submitted form" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "nino[0]" -> ""
      )

      when(mockDirectorDetailService.getDirectorDetails()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(testDirectors))

      when(mockDirectorDetailService.createDisplayNamesMap(Matchers.any()))
        .thenReturn(directorMap)

      AuthBuilder.submitWithAuthorisedUser(testController.submitDirectorDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return a SEE_OTHER and redirect to the PAYE Contact page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody()

      when(mockDirectorDetailService.submitNinos(Matchers.any())(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthBuilder.submitWithAuthorisedUser(testController.submitDirectorDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/register-for-paye/company-contact")
      }
    }
  }
}
