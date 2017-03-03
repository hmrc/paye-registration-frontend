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
import fixtures.{PAYERegistrationFixture, S4LFixture}
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CompanyDetailsService, PAYEContactService}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PAYEContactDetailsControllerSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture {
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockPAYEContactService = mock[PAYEContactService]
  val mockMessagesApi = mock[MessagesApi]

  class Setup {
    val testController = new PAYEContactDetailsCtrl {
      override val companyDetailsService = mockCompanyDetailsService
      override val payeContactService = mockPAYEContactService
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val authConnector = mockAuthConnector
    }
  }

  "payeContactDetails" should {
    "return a SEE_OTHER if user is not authorised" in new Setup {
      val result = testController.payeContactDetails()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return an OK with data from registration" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(Matchers.any())).thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.getPAYEContact(Matchers.any[HeaderCarrier]()))
        .thenReturn(validPAYEContactView)

      AuthBuilder.showWithAuthorisedUser(testController.payeContactDetails, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }

    "return an OK without data" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(Matchers.any())).thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPAYEContactService.getPAYEContact(Matchers.any()))
        .thenReturn(emptyPAYEContactView)

      AuthBuilder.showWithAuthorisedUser(testController.payeContactDetails, mockAuthConnector) {
        (result: Future[Result])  =>
          status(result) shouldBe OK
      }
    }
  }

  "submitPAYEContactDetails" should {
    "return a SEE_OTHER if the user is not authorised" in new Setup {
      val result = testController.submitPAYEContactDetails()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return a BAD_REQUEST if there is problem with the submitted form, no name" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> ""
      )

      when(mockCompanyDetailsService.getCompanyDetails(Matchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return a BAD_REQUEST if there is problem with the submitted form, no contact details" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "teeeeeeest"
      )

      when(mockCompanyDetailsService.getCompanyDetails(Matchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "tata",
        "digitalContact.contactEmail" -> "tata@test.com"
      )

      when(mockPAYEContactService.submitPayeContactDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return a SEE_OTHER and redirect to the Employing Staff page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "tata",
        "digitalContact.contactEmail" -> "tata@test.com"
      )

      when(mockPAYEContactService.submitPayeContactDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthBuilder.submitWithAuthorisedUser(testController.submitPAYEContactDetails, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/register-for-paye/employing-staff")
      }
    }
  }
}
