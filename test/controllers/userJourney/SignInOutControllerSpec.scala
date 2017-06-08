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
import enums.AccountTypes
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{IncorporationInformationService, CurrentProfileService, PAYERegistrationService}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

class SignInOutControllerSpec extends PAYERegSpec {

  val mockCurrentProfileService = mock[CurrentProfileService]
  val mockCoHoAPIService = mock[IncorporationInformationService]
  val mockPAYERegService = mock[PAYERegistrationService]

  class Setup {
    val controller = new SignInOutCtrl {
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"
    }
  }

  "Calling the postSignIn action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.postSignIn()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "redirect the user to the Company Registration post-sign-in action" in new Setup {
      when(mockPAYERegService.getAccountAffinityGroup(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[AuthContext]()))
        .thenReturn(AccountTypes.InvalidAccountType)

      AuthBuilder.showWithAuthorisedUser(controller.postSignIn, mockAuthConnector) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/post-sign-in")
      }
    }
  }

  "signOut" should {
    "redirect to the exit questionnaire and clear the session" in new Setup {
      AuthBuilder.showWithAuthorisedUser(controller.signOut, mockAuthConnector) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/questionnaire")
      }
    }
  }
}
