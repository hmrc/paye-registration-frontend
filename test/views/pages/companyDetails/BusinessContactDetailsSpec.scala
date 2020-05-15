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

import forms.companyDetails.BusinessContactDetailsForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.DigitalContactDetails
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.companyDetails.businessContactDetails

class BusinessContactDetailsSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = mockMessagesApi

  val testCompanyName = "Test company limited"

  val testBusinessContact = BusinessContactDetailsForm.form
  DigitalContactDetails(
    Some("test@test.co.uk"),
    Some("01234567890"),
    Some("07111222333")
  )


  "The Business contact details screen" should {
    lazy val view = businessContactDetails(testBusinessContact, testCompanyName)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text mustBe messagesApi("pages.businessContact.description", testCompanyName)
    }
  }
}
