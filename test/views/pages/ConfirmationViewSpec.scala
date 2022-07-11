/*
 * Copyright 2022 HM Revenue & Customs
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

package views.pages

import helpers.{PayeComponentSpec, PayeFakedApp}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.BaseSelectors
import views.html.pages.confirmation

class ConfirmationViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {


  object Selectors extends BaseSelectors

  implicit val appConfig = mockAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = mockMessagesApi
  lazy val view = app.injector.instanceOf[confirmation]

  val ackRef = "ackRef"
  val displayNextTaxYearContent = true
  val contactDate = "contactDate"

  lazy val page: Html = {
    val view = app.injector.instanceOf[confirmation]

    view(ackRef, displayNextTaxYearContent, contactDate)(
      request,
      messagesApi.preferred(request),
      mockAppConfig
    )
  }

  "The questionnaire link" should {
    lazy val document = Jsoup.parse(page.body)
    "show the correct content" in {
      document.getElementById("Questionnaire").text() mustBe "What do you think of the service? (usually takes one minute to complete) (opens in new tab)"
      document.getElementById("Questionnaire").attr("href") mustBe "http://localhost:9970/register-your-company/questionnaire"
      document.getElementById("Questionnaire").attr("target") mustBe "_blank"
    }
  }
}
