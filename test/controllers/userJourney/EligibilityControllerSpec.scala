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

import builders.AuthBuilder
import connectors.{KeystoreConnect, PAYERegistrationConnector}
import enums.DownstreamOutcome
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import models.view.{CompanyEligibility, DirectorEligibility, Eligibility}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import services.{EligibilityService, EligibilitySrv}
import testHelpers.PAYERegSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class EligibilityControllerSpec extends PAYERegSpec {
  val mockEligibilityService = mock[EligibilityService]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]

  class Setup {
    val controller = new EligibilityCtrl {
      override val eligibilityService: EligibilitySrv = mockEligibilityService
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"
      override val keystoreConnector: KeystoreConnect = mockKeystoreConnector
      override val payeRegistrationConnector = mockPayeRegistrationConnector

      override protected def authConnector = mockAuthConnector

      override def messagesApi = mockMessages

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

  val regId = "12345"
  val fakeRequest = FakeRequest("GET", "/")

  val validEligibilityModel = Eligibility(Some(CompanyEligibility(false)), Some(DirectorEligibility(false)))
  val validEmptyModel = Eligibility(None, None)


  "calling the companyEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.companyEligibility()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEligibilityModel))

      AuthBuilder.showWithAuthorisedUser(controller.companyEligibility, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEmptyModel))

      AuthBuilder.showWithAuthorisedUser(controller.companyEligibility, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the submitCompanyEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitCompanyEligibility()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyEligibility(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Ineligibility page when a user enters YES answer" in new Setup {
      when(mockEligibilityService.saveCompanyEligibility(ArgumentMatchers.eq(regId), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(DownstreamOutcome.Success)
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyEligibility(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "true"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/you-cant-register-online"
      }
    }

    "redirect to the Director Eligibility page when a user enters NO answer" in new Setup {
      when(mockEligibilityService.saveCompanyEligibility(ArgumentMatchers.eq(regId), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(DownstreamOutcome.Success)
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyEligibility(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/pay-non-cash-incentive-awards"
      }
    }
  }

  "calling the directorEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.directorEligibility()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEligibilityModel))

      AuthBuilder.showWithAuthorisedUser(controller.directorEligibility, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEligibilityService.getEligibility(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEmptyModel))

      AuthBuilder.showWithAuthorisedUser(controller.directorEligibility, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the submitDirectorEligibility action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitDirectorEligibility()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitDirectorEligibility(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Ineligibility page when a user enters YES answer" in new Setup {
      when(mockEligibilityService.saveDirectorEligibility(ArgumentMatchers.eq(regId), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(DownstreamOutcome.Success)
      AuthBuilder.submitWithAuthorisedUser(controller.submitDirectorEligibility(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "true"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/you-cant-register-online"
      }
    }

    "redirect to the Director Eligibility page when a user enters NO answer" in new Setup {
      when(mockEligibilityService.saveDirectorEligibility(ArgumentMatchers.eq(regId), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(DownstreamOutcome.Success)
      AuthBuilder.submitWithAuthorisedUser(controller.submitDirectorEligibility(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEligible" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/use-subcontractors-construction-industry"
      }
    }
  }

  "calling the ineligible action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.ineligible()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user" in new Setup {

      AuthBuilder.showWithAuthorisedUser(controller.ineligible, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the feedback action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.questionnaire()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 303 and redirect to Company Registration for an authorised user" in new Setup {

      AuthBuilder.showWithAuthorisedUser(controller.questionnaire, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.SEE_OTHER
          response.header.headers("Location") shouldBe "testUrl/testUri/questionnaire"
      }
    }
  }

}
