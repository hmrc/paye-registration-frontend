/*
 * Copyright 2018 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}

import scala.concurrent.Future
import uk.gov.hmrc.http.{CoreGet, CorePost, HttpGet, HttpResponse}

class FeedbackControllerSpec extends PAYERegSpec {

  class Setup {
    val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

    val controller = new FeedbackController(messagesApi) {
      override val http: CorePost = mockWSHttp

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
    }
  }

  "GET /feedback" should {
    val fakeRequest = FakeRequest("GET", "/")

    "return feedback page" in new Setup {
      val result = controller.feedbackShow(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "capture the referrer in the session on initial session on the feedback load" in new Setup {
      val result = controller.feedbackShow(fakeRequest.withHeaders("Referer" -> "Blah"))
      status(result) shouldBe Status.OK
    }
  }

  "POST /feedback" should {
    val fakeRequest = FakeRequest("GET", "/")
    val fakePostRequest = FakeRequest("POST", "/register-for-paye/feedback").withFormUrlEncodedBody("test" -> "test")
    "return form with thank you for valid selections" in new Setup {
      when(mockWSHttp.POSTForm[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
        Future.successful(HttpResponse(Status.OK, responseString = Some("1234"))))

      val result = controller.submitFeedback(fakePostRequest)
      redirectLocation(result) shouldBe Some(routes.FeedbackController.thankyou().url)
    }

    "return form with errors for invalid selections" in new Setup {
      when(mockWSHttp.POSTForm[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
        Future.successful(HttpResponse(Status.BAD_REQUEST, responseString = Some("<p>:^(</p>"))))
      val result = controller.submitFeedback(fakePostRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return error for other http code back from contact-frontend" in new Setup {
      when(mockWSHttp.POSTForm[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
        Future.successful(HttpResponse(418))) // 418 - I'm a teapot
      val result = controller.submitFeedback(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return internal server error when there is an empty form" in new Setup {
      when(mockWSHttp.POSTForm[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
        Future.successful(HttpResponse(Status.OK, responseString = Some("1234"))))

      val result = controller.submitFeedback(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "GET /feedback/thankyou" should {
    "should return the thank you page" in new Setup {
      val fakeRequest = FakeRequest("GET", "/")
      val result = controller.thankyou(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
