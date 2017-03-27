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

package controllers.errors

import builders.AuthBuilder
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testHelpers.PAYERegSpec

import scala.concurrent.Future

class ErrorControllerSpec extends PAYERegSpec {

  val fakeRequest = FakeRequest("GET", "/")

  class Setup {
    val controller = new ErrorCtrl {
      override val keystoreConnector = mockKeystoreConnector
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  "GET /start" should {
    "return 200" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.ineligible, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.OK
      }
    }
    "return HTML" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.ineligible, mockAuthConnector) {
        (result: Future[Result]) =>
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
      }
    }
  }

}