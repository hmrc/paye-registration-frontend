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

package views.pages

import forms.natureOfBuinessDetails.NatureOfBusinessForm
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.natureOfBusiness

class NatureOfBusinessViewSpec extends PAYERegSpec with I18nSupport {

  implicit val request = FakeRequest()
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]
  val companyName = "Company Limited"

  "The nature of business screen" should {
    lazy val view = natureOfBusiness(NatureOfBusinessForm.form, companyName)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.natureOfBusiness.heading", companyName)
    }

    "have the correct lede paragraph" in {
      document.getElementById("lede-paragraph").text shouldBe messagesApi("pages.natureOfBusiness.lede", companyName)
    }

    "have the correct hint text" in {
      document.getElementById("hint-text").text shouldBe messagesApi("pages.natureOfBusiness.hint")
    }

    "have the correct company name" when {
      "placed into the title" in {
        document.getElementById("company-title").text shouldBe companyName
      }

      "placed into the lede paragraph" in {
        document.getElementById("company-lede").text shouldBe companyName
      }
    }
  }
}
