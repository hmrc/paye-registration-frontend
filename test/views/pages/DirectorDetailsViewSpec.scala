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

package views.pages

import java.util.Locale

import forms.directorDetails.DirectorDetailsForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.{Ninos, UserEnteredNino}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.directorDetails

class DirectorDetailsViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {

  implicit val appConfig = mockAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = mockMessagesApi
  implicit val mockMessages = mockMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  val d1 = "Toto Tata (id 0)"
  val d2 = "Bib Bloup (id 1)"
  val d3 = "Pill Poll (id 2)"
  val d4 = "Jag Land (id 3)"
  val d5 = "Grep Sed (id 4)"

  val userNinos = Ninos(List(UserEnteredNino("0", Some("ZY123456A"))))
  val directorMap = Map("0" -> "Toto Tata (id 0)")

  val userNinosMany = Ninos(
    List(
      UserEnteredNino("1", Some("Nino for ID 1")),
      UserEnteredNino("0", Some("Nino for ID 0")),
      UserEnteredNino("4", Some("Nino for ID 4")),
      UserEnteredNino("3", None),
      UserEnteredNino("2", None)
    )
  )

  val directorMapMany = Map(
    "0" -> d1,
    "1" -> d2,
    "2" -> d3,
    "3" -> d4,
    "4" -> d5
  )


  "The confirm your Director Details screen with one director" should {
    lazy val view = directorDetails(DirectorDetailsForm.form.fill(userNinos), directorMap)
    lazy val document = Jsoup.parse(view.body)

    "have the title for a single director" in {
      document.getElementById("pageHeading").text mustBe mockMessages("pages.directorDetails.description")
    }

    "display the directors name and prepopped Nino" in {
      document.getElementsByClass("form-field").get(0).text mustBe "Toto Tata (id 0)'s National Insurance number For example, QQ 12 34 56 C"
      document.getElementsByAttributeValueContaining("value", "ZY 12 34 56 A").size mustBe 1
    }

    "show no more directors" in {
      document.getElementsByClass("form-field").size mustBe 1
    }
  }

  "The confirm your Director Details screen with many directors" should {
    lazy val view = directorDetails(DirectorDetailsForm.form.fill(userNinosMany), directorMapMany)
    lazy val document = Jsoup.parse(view.body)

    "have the title for many directors" in {
      document.getElementById("pageHeading").text mustBe mockMessages("pages.directorDetails.description")
    }

    "have all directors shown" in {
      val list = document.getElementsByClass("form-field")

      def get(n: Int) = list.get(n).text

      get(0) mustBe s"$d1's National Insurance number For example, QQ 12 34 56 C"
      get(1) mustBe s"$d2's National Insurance number"
      get(2) mustBe s"$d3's National Insurance number"
      get(3) mustBe s"$d4's National Insurance number"
      get(4) mustBe s"$d5's National Insurance number"
      document.getElementsByClass("form-field").size mustBe 5
      document.getElementsByClass("form-field").size mustBe 5
    }

    "only prepop the 3 fields that have data" in {
      document.getElementsByAttributeValueContaining("value", "Ni no").size mustBe 3
    }
  }

}
