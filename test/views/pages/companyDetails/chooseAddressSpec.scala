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

import forms.companyDetails.PPOBForm
import forms.payeContactDetails.CorrespondenceAddressForm
import models.Address
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.companyDetails.{ppobAddress => PPOBAddressPage}
import views.html.pages.payeContact.{correspondenceAddress => CorrespondenceAddressPage}

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

  val testCorrespondenceAddress =
    Address(
      "testCAL65",
      "testCAL66",
      Some("testCAL33"),
      Some("testCAL44"),
      Some("testCAPostCode"),
      None
    )

  val testListPrepopAddresses = Seq(
    Address(
      "testPPAL01",
      "testPPAL02",
      Some("testPPAL03"),
      Some("testPPAL04"),
      Some("testPPAPostCode01"),
      None
    ),
    Address(
      "testPPAL11",
      "testPPAL12",
      Some("testPPAL13"),
      Some("testPPAL14"),
      Some("testPPAPostCode11"),
      None
    )
  )

  "The PPOB Address screen without PPOB Address" should {
    lazy val view = PPOBAddressPage(PPOBForm.form, Some(testROAddress), None)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.ppobAddress.heading")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("value") shouldBe "roAddress"
    }

    "have the correct text for radio button roAddress" in {
      document.getElementById("ro-address-line-1").text shouldBe "testL1"
    }

    "not have the radio button ppobAddress" in {
      an[NullPointerException] shouldBe thrownBy(document.getElementById("chosenAddress-ppobaddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("chosenAddress-other").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("chosenAddress-other").attr("value") shouldBe "other"
    }
  }

  "The PPOB Address screen with PPOB Address" should {
    lazy val view = PPOBAddressPage(PPOBForm.form, Some(testROAddress), Some(testPPOBAddress))
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button ppobAddress" in {
      document.getElementById("chosenAddress-ppobaddress").attr("value") shouldBe "ppobAddress"
    }

    "have the correct text for radio button ppobAddress" in {
      document.getElementById("ppob-address-line-1").text shouldBe "testL65"
    }
  }

  "The Correspondence Address screen without Correspondence Address" should {
    lazy val view = CorrespondenceAddressPage(CorrespondenceAddressForm.form, Some(testROAddress), None, Seq.empty)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.correspondenceAddress.title")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("chosenAddress-roaddress").attr("value") shouldBe "roAddress"
    }

    "have the correct text for radio button roAddress" in {
      document.getElementById("ro-address-line-1").text shouldBe "testL1"
    }

    "not have the radio button correspondenceAddress" in {
      an[NullPointerException] shouldBe thrownBy(document.getElementById("chosenAddress-correspondenceaddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("chosenAddress-other").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("chosenAddress-other").attr("value") shouldBe "other"
    }
  }

  "The Correspondence Address screen with Correspondence Address" should {
    lazy val view = CorrespondenceAddressPage(CorrespondenceAddressForm.form, Some(testROAddress), Some(testCorrespondenceAddress), Seq.empty)
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button correspondenceAddress" in {
      document.getElementById("chosenAddress-correspondenceaddress").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button correspondenceAddress" in {
      document.getElementById("chosenAddress-correspondenceaddress").attr("value") shouldBe "correspondenceAddress"
    }

    "have the correct text for radio button correspondenceAddress" in {
      document.getElementById("correspondence-address-line-1").text shouldBe "testCAL65"
    }
  }

  "The Correspondence Address screen with Prepop Addresses" should {
    lazy val view = CorrespondenceAddressPage(CorrespondenceAddressForm.form, Some(testROAddress), Some(testCorrespondenceAddress), testListPrepopAddresses)
    lazy val document = Jsoup.parse(view.body)

    "have the correct name for radio button prepopaddress0" in {
      document.getElementById("chosenAddress-prepopaddress0").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button prepopaddress0" in {
      document.getElementById("chosenAddress-prepopaddress0").attr("value") shouldBe "prepopAddress0"
    }

    "have the correct text for radio button prepopaddress0" in {
      document.getElementById("prepopaddress0-address-line-1").text shouldBe "testPPAL01"
    }

    "have the correct name for radio button prepopaddress1" in {
      document.getElementById("chosenAddress-prepopaddress1").attr("name") shouldBe "chosenAddress"
    }

    "have the correct value for radio button prepopaddress1" in {
      document.getElementById("chosenAddress-prepopaddress1").attr("value") shouldBe "prepopAddress1"
    }

    "have the correct text for radio button prepopaddress1" in {
      document.getElementById("prepopaddress1-address-line-1").text shouldBe "testPPAL11"
    }
  }
}
