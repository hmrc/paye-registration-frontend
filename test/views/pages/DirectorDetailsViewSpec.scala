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

package views.pages

import forms.directorDetails.DirectorDetailsForm
import models.view.{Ninos, UserEnteredNino}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.directorDetails

class DirectorDetailsViewSpec extends PAYERegSpec with I18nSupport {

  implicit val request = FakeRequest()
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]

  val d1 = "Henri Lay (id 0)"
  val d2 = "Chris Walker (id 1)"
  val d3 = "Tom Stacey (id 2)"
  val d4 = "Jhansi Tummala (id 3)"
  val d5 = "Chris Poole (id 4)"

  val userNinos = Ninos(List(UserEnteredNino("0", Some("ZY123456A"))))
  val directorMap = Map("0" -> "Henri Lay (id 0)")

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
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.directorDetails.title1Director")
    }

    "display the directors name and prepopped Nino" in {
      document.getElementsByClass("form-field").get(0).text shouldBe "Henri Lay (id 0)'s National Insurance number For example, QQ 12 34 56 C"
      document.getElementsByAttributeValueContaining("value", "ZY 12 34 56 A").size shouldBe 1
    }

    "show no more directors" in {
      document.getElementsByClass("form-field").size shouldBe 1
    }
  }

  "The confirm your Director Details screen with many directors" should {
    lazy val view = directorDetails(DirectorDetailsForm.form.fill(userNinosMany), directorMapMany)
    lazy val document = Jsoup.parse(view.body)

    "have the title for many directors" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.directorDetails.titleMultipleDirectors")
    }

    "have all directors shown" in {
      val list = document.getElementsByClass("form-field")
      def get(n: Int) = list.get(n).text
      get(0) shouldBe s"$d1's National Insurance number For example, QQ 12 34 56 C"
      get(1) shouldBe s"$d2's National Insurance number"
      get(2) shouldBe s"$d3's National Insurance number"
      get(3) shouldBe s"$d4's National Insurance number"
      get(4) shouldBe s"$d5's National Insurance number"
      document.getElementsByClass("form-field").size shouldBe 5
      document.getElementsByClass("form-field").size shouldBe 5
    }

    "only prepop the 3 fields that have data" in {
      document.getElementsByAttributeValueContaining("value", "Ni no").size shouldBe 3
    }
  }

}
