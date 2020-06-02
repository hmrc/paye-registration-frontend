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

package views.pages

import forms.natureOfBuinessDetails.NatureOfBusinessForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.natureOfBusiness

class NatureOfBusinessViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {
  implicit val appConfig = mockAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi : MessagesApi = mockMessagesApi

  "The nature of business screen" should {
    lazy val view = natureOfBusiness(NatureOfBusinessForm.form)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text mustBe messagesApi("pages.natureOfBusiness.description")
    }

    "have the correct label text" in {
      document.getElementsByClass("form-label").first().text() mustBe messagesApi("pages.natureOfBusiness.textArea.label")
    }
  }
}
