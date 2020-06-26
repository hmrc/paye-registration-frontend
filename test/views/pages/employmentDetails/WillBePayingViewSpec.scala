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

package views.pages.employmentDetails

import java.time.LocalDate

import forms.employmentDetails.EmployingStaffForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.WillBePaying
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.employmentDetails.willBePaying

class WillBePayingViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {
  implicit val appConfig = mockAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = mockMessagesApi

  "The Will Be Paying screen" should {
    "display the dynamic radio button 'beforeNewTaxYear' when 'willBePaying' is Yes" when {
      val data = WillBePaying(true, None)

      "the current date is 6th Feb" in {
        val now = LocalDate.of(2017, 2, 6)
        lazy val view = willBePaying(EmployingStaffForm.form(now).fill(data), 116, now)
        lazy val document = Jsoup.parse(view.body)

        document.getElementById("beforeNewTaxYear-true").attr("value") mustBe "true"
      }
      "the current date is 5th Apr" in {
        val now = LocalDate.of(2017, 4, 5)
        lazy val view = willBePaying(EmployingStaffForm.form(now).fill(data), 116, now)
        lazy val document = Jsoup.parse(view.body)

        document.getElementById("beforeNewTaxYear-true").attr("value") mustBe "true"
      }
      "the current date is between 6th Feb and 5th Apr" in {
        val now = LocalDate.of(2017, 2, 7)
        lazy val view = willBePaying(EmployingStaffForm.form(now).fill(data), 116, now)
        lazy val document = Jsoup.parse(view.body)

        document.getElementById("beforeNewTaxYear-true").attr("value") mustBe "true"
      }
    }

    "not be loading the dynamic radio button 'beforeNewTaxYear'" when {
      val data = WillBePaying(false, None)

      "the current date is 5th Feb" in {
        val now = LocalDate.of(2017, 2, 5)
        lazy val view = willBePaying(EmployingStaffForm.form(now).fill(data), 116, now)
        lazy val document = Jsoup.parse(view.body)

        a[NullPointerException] mustBe thrownBy(document.getElementById("beforeNewTaxYear-true").attr("value"))
      }
      "the current date is 6th Apr" in {
        val now = LocalDate.of(2017, 4, 6)
        lazy val view = willBePaying(EmployingStaffForm.form(now).fill(data), 116, now)
        lazy val document = Jsoup.parse(view.body)

        a[NullPointerException] mustBe thrownBy(document.getElementById("beforeNewTaxYear-true").attr("value"))
      }
      "the current date is not between 6th Feb and 5th Apr" in {
        val now = LocalDate.of(2017, 8, 7)
        lazy val view = willBePaying(EmployingStaffForm.form(now).fill(data), 116, now)
        lazy val document = Jsoup.parse(view.body)

        a[NullPointerException] mustBe thrownBy(document.getElementById("beforeNewTaxYear-true").attr("value"))
      }
    }
  }
}
