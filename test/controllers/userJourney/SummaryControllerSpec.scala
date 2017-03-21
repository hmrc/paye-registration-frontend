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
import fixtures.PAYERegistrationFixture
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.Helpers._
import services.SummaryService
import testHelpers.PAYERegSpec

import scala.concurrent.Future

class SummaryControllerSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockSummaryService = mock[SummaryService]

  class Setup {
    val controller = new SummaryCtrl {
      override val summaryService = mockSummaryService
      override val authConnector = mockAuthConnector
      override val keystoreConnector = mockKeystoreConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  "Calling summary to show the summary page" should {
    "show the summary page when a valid model is returned from the microservice" in new Setup {
      mockFetchCurrentProfile()
      when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validSummaryView))

      AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body().getElementById("pageHeading").text() shouldBe "Check your answers"
          result.body.getElementById("tradingNameAnswer").text() shouldBe "tstTrade"
      }
    }

    "return an Internal Server Error response when no valid model is returned from the microservice" in new Setup {
      mockFetchCurrentProfile()
      when(mockSummaryService.getRegistrationSummary(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.failed(new InternalError()))

      AuthBuilder.showWithAuthorisedUser(controller.summary, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling submitRegistration" should {
    "show the confirmation page" in new Setup {
      mockFetchCurrentProfile()

      when(mockSummaryService.submitRegistration(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful("ackRef"))

      AuthBuilder.showWithAuthorisedUser(controller.submitRegistration, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe "/register-for-paye/confirmation"
      }
    }
  }

}
