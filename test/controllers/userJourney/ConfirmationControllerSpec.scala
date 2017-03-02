/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.http.Status.OK
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import testHelpers.PAYERegSpec

import scala.concurrent.Future

class ConfirmationControllerSpec extends PAYERegSpec {

  class Setup {
    val controller = new ConfirmationCtrl {
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  "showConfirmation" should {
    "display the confirmation page" in new Setup {
      AuthBuilder.showWithAuthorisedUser(controller.showConfirmation, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe OK
      }
    }
  }

}
