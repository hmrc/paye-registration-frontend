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

package views.pages.companyDetails

import forms.companyDetails.PPOBForm
import forms.payeContactDetails.CorrespondenceAddressForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.Address
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.test.FakeRequest
import views.BaseSelectors
import views.html.pages.companyDetails.ppobAddress
import views.html.pages.payeContact.correspondenceAddress

import java.util.Locale

class ChooseAddressSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {

  object Selectors extends BaseSelectors

  implicit val appConfig = injAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = injMessagesApi
  implicit val mockMessages = injMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  val testROAddress =
    "ro" -> "testL1, testL2, testL3, testL4, testPostCode"

  val testPPOBAddress =
    "ppob" -> "testL65, testL66, testL33, testL44, testPostCode"

  val testCorrespondenceAddress =
    "correspondence" -> "testCAL65, testCAL66, testCAL33, testCAL44, testCAPostCode"

  val testOther = "other" -> "Other"

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
    lazy val view = app.injector.instanceOf[ppobAddress]
    lazy val document = Jsoup.parse(view(PPOBForm.form, Map(testROAddress, testOther), None).body)

    "have the correct title" in {
      document.select(Selectors.h1).text() mustBe mockMessages("pages.ppobAddress.description")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("chosenAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("chosenAddress").attr("value") mustBe "roAddress"
    }

    "not have the radio button ppobAddress" in {
      an[NullPointerException] mustBe thrownBy(document.getElementById("ppobAddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("otherAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("otherAddress").attr("value") mustBe "otherAddress"
    }
  }

  "The PPOB Address screen with PPOB Address" should {
    lazy val view = app.injector.instanceOf[ppobAddress]
    lazy val document = Jsoup.parse(view(PPOBForm.form, Map(testPPOBAddress), None).body)

    "have the correct name for radio button ppobAddress" in {
      document.getElementById("chosenAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button ppobAddress" in {
      document.getElementById("chosenAddress").attr("value") mustBe "ppobAddress"
    }
  }

  "The Correspondence Address screen without Correspondence Address" should {
    lazy val view = app.injector.instanceOf[correspondenceAddress]
    lazy val document = Jsoup.parse(view(CorrespondenceAddressForm.form, Map(testPPOBAddress, testROAddress, testOther),None).body)

    "have the correct title" in {
      document.select(Selectors.h1).text() mustBe mockMessages("pages.correspondenceAddress.description")
    }

    "have the correct name for radio button roAddress" in {
      document.getElementById("roAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button roAddress" in {
      document.getElementById("roAddress").attr("value") mustBe "roAddress"
    }

    "have the correct name for radio button ppobAddress" in {
      document.getElementById("chosenAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button ppobAddress" in {
      document.getElementById("chosenAddress").attr("value") mustBe "ppobAddress"
    }

    "not have the radio button correspondenceAddress" in {
      an[NullPointerException] mustBe thrownBy(document.getElementById("correspondenceAddress").attr("name"))
    }

    "have the correct name for radio button other" in {
      document.getElementById("otherAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button other" in {
      document.getElementById("otherAddress").attr("value") mustBe "otherAddress"
    }
  }

  "The Correspondence Address screen with Correspondence Address" should {
    lazy val view = app.injector.instanceOf[correspondenceAddress]
    lazy val document = Jsoup.parse(view(CorrespondenceAddressForm.form, Map(testCorrespondenceAddress), None).body)

    "have the correct name for radio button correspondenceAddress" in {
      document.getElementById("chosenAddress").attr("name") mustBe "chosenAddress"
    }

    "have the correct value for radio button correspondenceAddress" in {
      document.getElementById("chosenAddress").attr("value") mustBe "correspondenceAddress"
    }
  }
}
