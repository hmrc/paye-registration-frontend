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

import forms.companyDetails.ChooseAddressForm
import models.view.{Address, AddressChoice, ChosenAddress}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.companyDetails.{chooseAddress => PPOBAddressPage}

class chooseAddressSpec extends PAYERegSpec with I18nSupport {
  implicit val request = FakeRequest()
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]

  val testROAddress =
    Address(
      "testL1",
      "testL2",
      Some("testL3"),
      Some("testL4"),
      Some("testPostCode"),
      None
    )

  val testPPOBAddress =
    Address(
      "testL65",
      "testL66",
      Some("testL33"),
      Some("testL44"),
      Some("testPostCode"),
      None
    )

  "The PPOB Address screen without PPOB Address" should {
    lazy val view = PPOBAddressPage(ChooseAddressForm.form, testROAddress, None)
    lazy val document = Jsoup.parse(view.body)

    "have the company company in the page title" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.ppobAddress.heading")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("value") shouldBe AddressChoice.roAddress.toString
    }

    "have the correct text for radio button roAddress" in {
      document.getElementById("ro-address-address-line-1").text shouldBe "testL1"
    }

    "not have the radio button ppobAddress" in {
      an[NullPointerException] shouldBe thrownBy(document.getElementById("chosenAddress-ppobaddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("chosenAddress-other").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("chosenAddress-other").attr("value") shouldBe AddressChoice.other.toString
    }
  }

  "The PPOB Address screen with PPOB Address" should {
    lazy val view = PPOBAddressPage(ChooseAddressForm.form, testROAddress, Some(testPPOBAddress))
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("value") shouldBe AddressChoice.ppobAddress.toString
    }

    "have the correct text for radio button ppobAddress" in {
      document.getElementById("ppob-address-address-line-1").text shouldBe "testL65"
    }
  }
}
