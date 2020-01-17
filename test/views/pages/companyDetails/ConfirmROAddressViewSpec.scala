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

package views.pages.companyDetails

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.Address
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.companyDetails.confirmROAddress

class ConfirmROAddressViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {

  implicit val request = FakeRequest()
  implicit lazy val messagesApi : MessagesApi = mockMessagesApi

  val testCompanyName = "Test company limited"
  val testAddress =
    Address(
      "testL1",
      "testL2",
      Some("testL3"),
      Some("testL4"),
      Some("testPostCode"),
      None
    )

  "The confirm your RO address screen" should {
    lazy val view = confirmROAddress(testCompanyName, testAddress)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text mustBe messagesApi("pages.confirmRO.description", testCompanyName)
    }

    "have the correct lede paragraph" in {
      document.getElementById("lead-paragraph").text mustBe messagesApi("pages.confirmRO.lede", testCompanyName)
    }

    "have the correct drop down text" in {
      document.getElementById("incorrect-address-Summary").text mustBe messagesApi("pages.confirmRO.help.link")
    }

    "have the correct drop down body text" in {
      document.getElementById("incorrect-address-Details").text.contains(messagesApi("pages.confirmRO.hiddenIntro.label")) mustBe true
      document.getElementById("incorrect-address-Details").text.contains(messagesApi("pages.common.companiesHouse.hiddenIntro.2")) mustBe true
    }

    "have the correct drop down body link text" in {
      document.getElementById("companies-house-link").text mustBe s"${messagesApi("pages.confirmRO.hiddenIntro.label")} ${messagesApi("app.common.linkHelperText")}"
    }
  }
}
