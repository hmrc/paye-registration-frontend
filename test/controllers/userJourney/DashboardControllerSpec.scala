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
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class DashboardControllerSpec extends PAYERegSpec {
  val fakeRequest = FakeRequest("GET", "/")
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]

  class Setup {
    val controller = new DashboardCtrl {
      override val authConnector = mockAuthConnector
      override val keystoreConnector = mockKeystoreConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val companyRegUrl = "testUrl"
      override val companyRegUri = "/testUri"
      override val payeRegistrationConnector = mockPayeRegistrationConnector

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          "12345",
          Some("Director"),
          CompanyRegistrationProfile("held", "txId"),
          "ENG"
        ))
      }
    }
  }

  "POST /dashboard" should {
    "return 303" in new Setup {
      val result = controller.dashboard(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to dashboard page" in new Setup {

      AuthBuilder.showWithAuthorisedUser(controller.dashboard, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controller.companyRegUrl}${controller.companyRegUri}/dashboard")
      }
    }
  }
}
