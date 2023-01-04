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

package views.pages

import forms.directorDetails.DirectorDetailsForm
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.{Ninos, UserEnteredNino}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.test.FakeRequest
import views.BaseSelectors
import views.html.pages.directorDetails

import java.util.Locale

class DirectorDetailsViewSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {

  object Selectors extends BaseSelectors

  implicit val appConfig = injAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = injMessagesApi
  implicit val mockMessages = injMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  val d1 = "Toto Tata (id 0)"
  val d2 = "Bib Bloup (id 1)"
  val d3 = "Pill Poll (id 2)"
  val d4 = "Jag Land (id 3)"
  val d5 = "Grep Sed (id 4)"

  val userNinos = Ninos(List(UserEnteredNino("0", Some("ZY123456A"))))
  val directorMap = Map("0" -> "Toto Tata (id 0)")

  val userNinosMany = Ninos(
    List(
      UserEnteredNino("1", Some("Ni no")),
      UserEnteredNino("0", Some("Ni no")),
      UserEnteredNino("4", Some("Ni no")),
      UserEnteredNino("3", Some("Ni no")),
      UserEnteredNino("2", Some("Ni no")),
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
    lazy val view = app.injector.instanceOf[directorDetails]
    lazy val document = Jsoup.parse(view(DirectorDetailsForm.form(directorMap).fill(userNinos), directorMap).body)

    "have the title for a single director" in {
      document.select(Selectors.h1).text() mustBe mockMessages("pages.directorDetails.description")
    }

    "display the directors name and prepopped Nino" in {
      document.getElementsByClass("form-field").size mustBe 1
    }
  }

  "The confirm your Director Details screen with many directors" should {
    lazy val view = app.injector.instanceOf[directorDetails]
    lazy val document = Jsoup.parse(view(DirectorDetailsForm.form(directorMapMany).fill(userNinosMany), directorMapMany).body)

    "have the title for many directors" in {
      document.select(Selectors.h1).text mustBe mockMessages("pages.directorDetails.description")
    }

    "display the directors names and prepopped Nino's" in {
      document.getElementsByClass("form-field").size mustBe 5
    }
  }

}
