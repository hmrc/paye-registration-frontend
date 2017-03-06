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
import enums.{AccountTypes, DownstreamOutcome}
import fixtures.PAYERegistrationFixture
import org.junit.Before
import services.{CoHoAPIService, CurrentProfileService, PAYERegistrationService}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.MessagesApi
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

class SignInOutControllerSpec extends PAYERegSpec with PAYERegistrationFixture with BeforeAndAfterEach {

  val mockCurrentProfileService = mock[CurrentProfileService]
  val mockCoHoAPIService = mock[CoHoAPIService]
  val mockPAYERegService = mock[PAYERegistrationService]

  class Setup {
    val controller = new SignInOutCtrl {
      override val authConnector = mockAuthConnector
      override val currentProfileService = mockCurrentProfileService
      override val coHoAPIService = mockCoHoAPIService
      override val payeRegistrationService = mockPAYERegService
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val compRegBaseURL: String = "testUrl"
    }
  }

  val fakeRequest = FakeRequest("GET", "/")

  override def beforeEach() {
    reset(mockPAYERegService)
  }

  "Calling the postSignIn action" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.postSignIn()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "force the user to create a new account" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(Matchers.any[HeaderCarrier](), Matchers.any[AuthContext]()))
        .thenReturn(AccountTypes.InvalidAccountType)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controller.compRegBaseURL}/register-your-company/post-sign-in")
      }
    }

    "show an Error page for an authorised user without a registration ID" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(Matchers.any[HeaderCarrier](), Matchers.any[AuthContext]()))
        .thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(Matchers.any()))
        .thenReturn(DownstreamOutcome.Failure)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID but no CoHo Company Details" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(Matchers.any[HeaderCarrier](), Matchers.any[AuthContext]()))
        .thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(Matchers.any())).thenReturn(DownstreamOutcome.Success)
      when(mockCoHoAPIService.fetchAndStoreCoHoCompanyDetails(Matchers.any())).thenReturn(DownstreamOutcome.Failure)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID and CoHo Company Details, with an error response from the microservice" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(Matchers.any[HeaderCarrier](), Matchers.any[AuthContext]()))
        .thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(Matchers.any())).thenReturn(DownstreamOutcome.Success)
      when(mockCoHoAPIService.fetchAndStoreCoHoCompanyDetails(Matchers.any())).thenReturn(DownstreamOutcome.Success)
      when(mockPAYERegService.assertRegistrationFootprint()(Matchers.any())).thenReturn(DownstreamOutcome.Failure)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the Trading Name page for an authorised user with a registration ID and CoHo Company Details, with PAYE Footprint correctly asserted" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(Matchers.any[HeaderCarrier](), Matchers.any[AuthContext]())).thenReturn(AccountTypes.Organisation)
      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(Matchers.any())).thenReturn(DownstreamOutcome.Success)
      when(mockCoHoAPIService.fetchAndStoreCoHoCompanyDetails(Matchers.any())).thenReturn(DownstreamOutcome.Success)
      when(mockPAYERegService.assertRegistrationFootprint()(Matchers.any())).thenReturn(DownstreamOutcome.Success)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.CompletionCapacityController.completionCapacity()}")
      }
    }
  }

}
