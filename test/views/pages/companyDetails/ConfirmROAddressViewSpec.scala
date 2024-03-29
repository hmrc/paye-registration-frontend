/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.test.FakeRequest
import views.BaseSelectors
import views.html.pages.companyDetails.confirmROAddress

import java.util.Locale

class ConfirmROAddressViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {

  object Selectors extends BaseSelectors

  implicit val appConfig = injAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = injMessagesApi
  implicit val mockMessages = injMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

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
    lazy val view = app.injector.instanceOf[confirmROAddress]
    lazy val document = Jsoup.parse(view(testCompanyName, testAddress).body)

    "have the correct title" in {
      document.select(Selectors.h1).text() mustBe mockMessages("pages.confirmRO.description", testCompanyName)
    }

    "have the correct drop down body link text" in {
      document.getElementById("companies-house-link").text mustBe s"${mockMessages("pages.confirmRO.hiddenIntro.label")} ${mockMessages("app.common.linkHelperText")}"
    }
  }
}
