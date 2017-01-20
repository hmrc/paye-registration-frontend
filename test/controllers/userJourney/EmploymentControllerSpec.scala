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

import testHelpers.PAYERegSpec
import play.api.http.Status
import play.api.test.FakeRequest
import builders.AuthBuilder
import play.api.mvc.Result
import scala.concurrent.Future

class EmploymentControllerSpec extends PAYERegSpec {
  class Setup {
    val controller = new EmploymentController {
      override val authConnector = mockAuthConnector
    }
  }

  val fakeRequest = FakeRequest("GET", "/")

  "calling the employingStaff action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.employingStaff()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user" in new Setup {
      AuthBuilder.showWithAuthorisedUser(controller.employingStaff, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the submitEmployingStaff action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitEmployingStaff()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Company Pension page when a user enters YES answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "currentYear" -> "true"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/paye-registration/company-pension"
      }
    }

    "redirect to the Summary page when a user enters NO answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "currentYear" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/paye-registration/summary"
      }
    }
  }

  "calling the companyPension action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.companyPension()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user" in new Setup {
      AuthBuilder.showWithAuthorisedUser(controller.companyPension, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the submitCompanyPension action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitCompanyPension()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyPension(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Summary page when a user enters YES answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyPension(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "pensionProvided" -> "true"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/paye-registration/summary"
      }
    }

    "redirect to the Summary page when a user enters NO answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyPension(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "pensionProvided" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/paye-registration/summary"
      }
    }
  }
}
