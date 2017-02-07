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

package views.pages.companyDetails

import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.companyDetails.ConfirmROAddress

class ConfirmROAddressViewSpec extends PAYERegSpec with I18nSupport {

  implicit val request = FakeRequest()
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]

  val testCompanyName = "Test company limited"

  "The confirm your RO address screen" should {
    lazy val view = ConfirmROAddress(testCompanyName)
    lazy val document = Jsoup.parse(view.body)

    "have the company company in the page title" in {
      document.getElementById("pageHeading").text.contains(testCompanyName) shouldBe true
    }

    "have the correct lede paragraph" in {
      document.getElementById("lead-paragraph").text shouldBe messagesApi("pages.confirmRO.lede")
    }

    "have the correct drop down text" in {
      document.getElementById("incorrect-address-Summary").text shouldBe messagesApi("pages.confirmRO.help.link")
    }

    "have the correct drop down body text" in {
      document.getElementById("incorrect-address-Details").text.contains(messagesApi("pages.confirmRO.help.body")) shouldBe true
    }

    "have the correct drop down body link text" in {
      document.getElementById("companies-house-link").text shouldBe messagesApi("app.common.companies-house")
    }
  }
}