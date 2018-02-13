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

package views.pages.companyDetails

import forms.companyDetails.PPOBForm
import forms.payeContactDetails.CorrespondenceAddressForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.Address
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.companyDetails.{ppobAddress => PPOBAddressPage}
import views.html.pages.payeContact.{correspondenceAddress => CorrespondenceAddressPage}

class chooseAddressSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {
  implicit val request = FakeRequest()
  implicit lazy val messagesApi : MessagesApi = mockMessagesApi

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

  val testCorrespondenceAddress =
    Address(
      "testCAL65",
      "testCAL66",
      Some("testCAL33"),
      Some("testCAL44"),
      Some("testCAPostCode"),
      None
    )

  val testListPrepopAddresses = Map(
    0 -> Address(
      "testPPAL01",
      "testPPAL02",
      Some("testPPAL03"),
      Some("testPPAL04"),
      Some("testPPAPostCode01"),
      None
    ),
    1 -> Address(
      "testPPAL11",
      "testPPAL12",
      Some("testPPAL13"),
      Some("testPPAL14"),
      Some("testPPAPostCode11"),
      None
    )
  )

  "The PPOB Address screen without PPOB Address" should {
    lazy val view = PPOBAddressPage(PPOBForm.form, Some(testROAddress), None, Map.empty[Int, Address])
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text mustBe messagesApi("pages.ppobAddress.description")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("value") mustBe "roAddress"
    }

    "have the correct text for radio button roAddress" in {
      document.getElementById("ro-address-line-1").text mustBe "testL1"
    }

    "not have the radio button ppobAddress" in {
      an[NullPointerException] mustBe thrownBy(document.getElementById("chosenAddress-ppobaddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("chosenAddress-other").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("chosenAddress-other").attr("value") mustBe "other"
    }
  }

  "The PPOB Address screen with PPOB Address" should {
    lazy val view = PPOBAddressPage(PPOBForm.form, Some(testROAddress), Some(testPPOBAddress), Map.empty[Int, Address])
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("value") mustBe "ppobAddress"
    }

    "have the correct text for radio button ppobAddress" in {
      document.getElementById("ppob-address-line-1").text mustBe "testL65"
    }
  }

  "The PPOB Address screen with Prepop Addresses" should {
    lazy val view = PPOBAddressPage(PPOBForm.form, Some(testROAddress), Some(testPPOBAddress), testListPrepopAddresses)
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button prepopaddress0" in {
      document.getElementById("chosenAddress-prepopaddress0").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button prepopaddress0" in {
      document.getElementById("chosenAddress-prepopaddress0").attr("value") mustBe "prepopAddress0"
    }

    "have the correct text for radio button prepopaddress0" in {
      document.getElementById("prepopaddress0-address-line-1").text mustBe "testPPAL01"
    }

    "have the correct name for radio button prepopaddress1" in {
      document.getElementById("chosenAddress-prepopaddress1").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button prepopaddress1" in {
      document.getElementById("chosenAddress-prepopaddress1").attr("value") mustBe "prepopAddress1"
    }

    "have the correct text for radio button prepopaddress1" in {
      document.getElementById("prepopaddress1-address-line-1").text mustBe "testPPAL11"
    }
  }

  "The Correspondence Address screen without Correspondence Address" should {
    lazy val view = CorrespondenceAddressPage(CorrespondenceAddressForm.form, Some(testROAddress), Some(testPPOBAddress), None, Map.empty[Int, Address])
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text mustBe messagesApi("pages.correspondenceAddress.description")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("value") mustBe "roAddress"
    }

    "have the correct text for radio button roAddress" in {
      document.getElementById("ro-address-line-1").text mustBe "testL1"
    }

    "have the correct name for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("value") mustBe "ppobAddress"
    }

    "have the correct text for radio button ppobAddress" in {
      document.getElementById("ppob-address-line-1").text mustBe "testL65"
    }

    "not have the radio button correspondenceAddress" in {
      an[NullPointerException] mustBe thrownBy(document.getElementById("chosenAddress-correspondenceaddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("chosenAddress-other").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("chosenAddress-other").attr("value") mustBe "other"
    }
  }

  "The Correspondence Address screen with Correspondence Address" should {
    lazy val view = CorrespondenceAddressPage(CorrespondenceAddressForm.form, Some(testROAddress), Some(testPPOBAddress), Some(testCorrespondenceAddress), Map.empty[Int, Address])
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button correspondenceAddress" in {
      document.getElementById("chosenAddress-correspondenceaddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button correspondenceAddress" in {
      document.getElementById("chosenAddress-correspondenceaddress").attr("value") mustBe "correspondenceAddress"
    }

    "have the correct text for radio button correspondenceAddress" in {
      document.getElementById("correspondence-address-line-1").text mustBe "testCAL65"
    }
  }

  "The Correspondence Address screen with Prepop Addresses" should {
    lazy val view = CorrespondenceAddressPage(CorrespondenceAddressForm.form, Some(testROAddress), Some(testPPOBAddress), Some(testCorrespondenceAddress), testListPrepopAddresses)
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button prepopaddress0" in {
      document.getElementById("chosenAddress-prepopaddress0").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button prepopaddress0" in {
      document.getElementById("chosenAddress-prepopaddress0").attr("value") mustBe "prepopAddress0"
    }

    "have the correct text for radio button prepopaddress0" in {
      document.getElementById("prepopaddress0-address-line-1").text mustBe "testPPAL01"
    }

    "have the correct name for radio button prepopaddress1" in {
      document.getElementById("chosenAddress-prepopaddress1").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button prepopaddress1" in {
      document.getElementById("chosenAddress-prepopaddress1").attr("value") mustBe "prepopAddress1"
    }

    "have the correct text for radio button prepopaddress1" in {
      document.getElementById("prepopaddress1-address-line-1").text mustBe "testPPAL11"
    }
  }
}
