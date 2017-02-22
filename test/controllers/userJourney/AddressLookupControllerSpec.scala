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
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AddressLookupService
import testHelpers.PAYERegSpec
import org.mockito.Mockito._

import scala.concurrent.Future

class AddressLookupControllerSpec extends PAYERegSpec {

  val mockAddressLookupService = mock[AddressLookupService]

  class Setup {
    val controller = new AddressLookupCtrl {
      implicit val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val addressLookupService = mockAddressLookupService
      protected def authConnector = mockAuthConnector
    }
  }

  val fakeRequest = FakeRequest("GET", "/")
  val fakeRequestWithId = FakeRequest("GET", "/returnFromLookup?id=123")

  "GET /redirectToLookup" should {
    "return 303" in new Setup {
      when(mockAddressLookupService.buildAddressLookupUrl())
        .thenReturn("testString")

      AuthBuilder.submitWithAuthorisedUser(controller.redirectToLookup, mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        (res: Future[Result]) =>
          status(res) shouldBe Status.SEE_OTHER
          redirectLocation(res) shouldBe Some("testString")
      }
    }
  }

  "GET /returnFromLookup" should {
    "return 303" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.returnFromLookup, mockAuthConnector, fakeRequestWithId.withFormUrlEncodedBody()){
        (result: Future[Result]) =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness()}")
      }
    }
  }

}
