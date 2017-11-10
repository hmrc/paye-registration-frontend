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

import models.Address
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.companyDetails.confirmROAddress

class ConfirmROAddressViewSpec extends PAYERegSpec with I18nSupport {

  implicit val request = FakeRequest()
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]

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
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.confirmRO.title")
    }

    "have the correct lede paragraph" in {
      document.getElementById("lead-paragraph").text shouldBe messagesApi("pages.confirmRO.lede", testCompanyName)
    }

    "have the correct drop down text" in {
      document.getElementById("incorrect-address-Summary").text shouldBe messagesApi("pages.confirmRO.help.link")
    }

    "have the correct drop down body text" in {
      document.getElementById("incorrect-address-Details").text.contains(messagesApi("pages.confirmRO.hiddenIntro.value")) shouldBe true
      document.getElementById("incorrect-address-Details").text.contains(messagesApi("pages.confirmRO.hiddenIntro.label")) shouldBe true
      document.getElementById("incorrect-address-Details").text.contains(messagesApi("pages.common.companiesHouse.hiddenIntro.2")) shouldBe true
    }

    "have the correct drop down body link text" in {
      document.getElementById("companies-house-link").text shouldBe s"${messagesApi("pages.confirmRO.hiddenIntro.label")} ${messagesApi("app.common.linkHiddenHelperText")}"
    }
  }
}
