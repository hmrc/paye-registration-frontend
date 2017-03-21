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
import models.external.{CompanyProfile, CurrentProfile}
import org.scalatest.BeforeAndAfterEach
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CoHoAPIService, CurrentProfileService, PAYERegistrationService}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

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
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"
    }
  }

  val fakeRequest = FakeRequest("GET", "/")
  def validCurrentProfile(status: String) = CurrentProfile("testRegId", None, CompanyProfile(status, "txId"), "en")


  override def beforeEach() {
    reset(mockPAYERegService)
  }

  "Calling the postSignIn action" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.postSignIn()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "force the user to create a new account" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]()))
        .thenReturn(AccountTypes.InvalidAccountType)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/post-sign-in")
      }
    }

    "show an Error page for an authorised user without a registration ID" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]()))
        .thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held")))

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID but no CoHo Company Details" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]()))
        .thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held")))

      when(mockCoHoAPIService.fetchAndStoreCoHoCompanyDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(DownstreamOutcome.Failure)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "show an Error page for an authorised user with a registration ID and CoHo Company Details, with an error response from the microservice" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]()))
        .thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held")))

      when(mockCoHoAPIService.fetchAndStoreCoHoCompanyDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(DownstreamOutcome.Success)

      when(mockPAYERegService.assertRegistrationFootprint(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(DownstreamOutcome.Failure)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the Trading Name page for an authorised user with a registration ID and CoHo Company Details, with PAYE Footprint correctly asserted" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]())).thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("held")))

      when(mockCoHoAPIService.fetchAndStoreCoHoCompanyDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(DownstreamOutcome.Success)

      when(mockPAYERegService.assertRegistrationFootprint(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(DownstreamOutcome.Success)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.CompletionCapacityController.completionCapacity()}")
      }
    }

    "redirect the user to the start of Incorporation and Corporation Tax if their Company Registration document has a status of 'draft'" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]())).thenReturn(AccountTypes.Organisation)

      when(mockCurrentProfileService.fetchAndStoreCurrentProfile(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCurrentProfile("draft")))

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/start")
      }
    }
  }

}
