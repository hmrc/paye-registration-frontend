/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.feedback

import config.AppConfig
import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.http.Status
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.{CoreGet, HttpResponse}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}

import scala.concurrent.Future

class FeedbackControllerSpec extends PayeComponentSpec with PayeFakedApp {

  class Setup extends CodeMocks {
    val controller = new FeedbackController(stubMessagesControllerComponents()) {
      override implicit val appConfig: AppConfig = mockAppConfig

      override def messagesApi = mockMessagesApi

      override val http = mockWSHttp

      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = new CachedStaticHtmlPartialRetriever {
        override def httpGet: CoreGet = mockWSHttp

        override def getPartialContent(url: String, templateParameters: Map[String, String], errorMessage: Html)(implicit request: RequestHeader): Html =
          Html("")
      }
      override implicit val formPartialRetriever: FormPartialRetriever = new FormPartialRetriever {
        override def crypto: (String) => String = ???

        override def httpGet: CoreGet = mockWSHttp

        override def getPartialContent(url: String,
                                       templateParameters: Map[String, String],
                                       errorMessage: Html)(implicit request: RequestHeader): Html = Html("")
      }
      override val sessionCookieCrypto: SessionCookieCrypto = mock[SessionCookieCrypto]
    }
  }

  "GET /feedback" should {
    val fakeRequest = FakeRequest("GET", "/")

    "return feedback page" in new Setup {
      val result: Future[Result] = controller.feedbackShow(fakeRequest)
      status(result) mustBe Status.OK
    }

    "capture the referrer in the session on initial session on the feedback load" in new Setup {
      val result: Future[Result] = controller.feedbackShow(fakeRequest.withHeaders("Referer" -> "Blah"))
      status(result) mustBe Status.OK
    }
  }

  "POST /feedback" should {
    val fakeRequest = FakeRequest("GET", "/")
    val fakePostRequest = FakeRequest("POST", "/register-for-paye/feedback").withFormUrlEncodedBody("test" -> "test")
    "return form with thank you for valid selections" in new Setup {
      val res = HttpResponse(Status.OK, responseString = Some("1234"))
      mockHttpPOSTForm[HttpResponse](fakePostRequest.uri, res)

      val result: Future[Result] = controller.submitFeedback(fakePostRequest)
      redirectLocation(result) mustBe Some(routes.FeedbackController.thankyou().url)
    }

    "return form with errors for invalid selections" in new Setup {
      val res = HttpResponse(Status.BAD_REQUEST, responseString = Some("<p>:^(</p>"))
      mockHttpPOSTForm[HttpResponse](fakePostRequest.uri, res)

      val result: Future[Result] = controller.submitFeedback(fakePostRequest)
      status(result) mustBe Status.BAD_REQUEST
    }

    "return error for other http code back from contact-frontend" in new Setup {
      val res = HttpResponse(418)
      mockHttpPOSTForm[HttpResponse](fakePostRequest.uri, res)

      val result: Future[Result] = controller.submitFeedback(fakePostRequest)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
    }

    "return internal server error when there is an empty form" in new Setup {
      val res = HttpResponse(Status.OK, responseString = Some("1234"))
      mockHttpPOSTForm[HttpResponse](fakePostRequest.uri, res)

      val result: Future[Result] = controller.submitFeedback(fakeRequest)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "GET /feedback/thankyou" should {
    "should return the thank you page" in new Setup {
      val fakeRequest = FakeRequest("GET", "/")

      val result: Future[Result] = controller.thankyou(fakeRequest)
      status(result) mustBe Status.OK
    }
  }
}
