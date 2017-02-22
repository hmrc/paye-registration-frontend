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

import models.view.{Summary, SummaryRow, SummarySection}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import views.html.pages.summary

class SummarySpec extends PAYERegSpec with I18nSupport {

  implicit val request = FakeRequest()
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]

  val suffixIdSectionHeading = "SectionHeading"
  val suffixIdQuestion = "Question"
  val suffixIdAnswer = "Answer"
  val suffixIdChangeLink = "ChangeLink"

  "The summary page" should {
    lazy val summaryModelNoTradingName = Summary(Seq())

    lazy val view = summary(summaryModelNoTradingName)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.summary.heading")
    }
  }

  "The summary page, Company Details section" should {

    lazy val summaryModelNoTradingName: Summary = Summary(
      Seq(
        SummarySection(
          id = "companyDetails",
          Seq(
            SummaryRow(
              id ="tradingName",
              answer = Left("noAnswerGiven"),
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
            ),
            SummaryRow(
              id = "roAddress",
              answer = Right("14 St Test Walk<br />Testley"),
              changeLink = None
            )
          )
        )
      )
    )

    lazy val view = summary(summaryModelNoTradingName)
    lazy val document = Jsoup.parse(view.body)

    "have Company Information in section title" in {
      document.getElementById(s"companyDetails$suffixIdSectionHeading").text shouldBe messagesApi("pages.summary.companyDetails.sectionHeading")
    }

    "have the correct question text for Other trading name" in {
      document.getElementById(s"tradingName$suffixIdQuestion").text shouldBe messagesApi("pages.summary.tradingName.question")
    }

    "have the correct answer text when no Other trading name" in {
      document.getElementById(s"tradingName$suffixIdAnswer").text shouldBe messagesApi("pages.summary.tradingName.answers.noAnswerGiven")
    }

    "have the correct change link for Other trading name" in {
      document.getElementById(s"tradingName$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.CompanyDetailsController.tradingName().toString
    }

    "have the correct question text for Registered office address" in {
      document.getElementById(s"roAddress$suffixIdQuestion").text shouldBe messagesApi("pages.summary.roAddress.question")
    }

    "have the correct answer text for Registered office address" in {
      document.getElementById(s"roAddress$suffixIdAnswer").text.contains("14 St Test Walk") &&
        document.getElementById(s"roAddress$suffixIdAnswer").text.contains("Testley") shouldBe true
    }

    "not have a change link for Registered office address" in {
      an[NullPointerException] shouldBe thrownBy(document.getElementById(s"roAddress$suffixIdChangeLink").text)
    }
  }
  "The summary page, Employment section" should {

    lazy val summaryModelEmployment = Summary(
      Seq(
        SummarySection(
          id = "employees",
          Seq(
            SummaryRow(
              id = "employees",
              answer = Left("false"),
              Some(controllers.userJourney.routes.EmploymentController.employingStaff())
            ),
            SummaryRow(
              id = "companyPension",
              answer = Left("false"),
              Some(controllers.userJourney.routes.EmploymentController.companyPension())
            ),
            SummaryRow(
              id = "subcontractors",
              answer = Left("false"),
              Some(controllers.userJourney.routes.EmploymentController.subcontractors())
            ),
            SummaryRow(
              id = "firstPaymentDate",
              Right("2/9/2016"),
              Some(controllers.userJourney.routes.EmploymentController.firstPayment())
            )
          )
        )
      )
    )

    lazy val view = summary(summaryModelEmployment)
    lazy val document = Jsoup.parse(view.body)

    "have Employment information in section title" in {
      document.getElementById(s"employees$suffixIdSectionHeading").text shouldBe messagesApi("pages.summary.employees.sectionHeading")
    }

    "have the correct question text for Employing staff" in {
      document.getElementById(s"employees$suffixIdQuestion").text shouldBe messagesApi("pages.summary.employees.question")
    }

    "have the correct answer text for Employing staff" in {
      document.getElementById(s"employees$suffixIdAnswer").text shouldBe messagesApi("pages.summary.employees.answers.false")
    }

    "have the correct change link for Employing staff" in {
      document.getElementById(s"employees$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.EmploymentController.employingStaff().toString
    }

    "have the correct question text for Company pension" in {
      document.getElementById(s"companyPension$suffixIdQuestion").text shouldBe messagesApi("pages.summary.companyPension.question")
    }

    "have the correct answer text for Company pension" in {
      document.getElementById(s"companyPension$suffixIdAnswer").text shouldBe messagesApi("pages.summary.companyPension.answers.false")
    }

    "have the correct change link for Company pension" in {
      document.getElementById(s"companyPension$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.EmploymentController.companyPension().toString
    }

    "have the correct question text for Subcontractors" in {
      document.getElementById(s"subcontractors$suffixIdQuestion").text shouldBe messagesApi("pages.summary.subcontractors.question")
    }

    "have the correct answer text for Subcontractors" in {
      document.getElementById(s"subcontractors$suffixIdAnswer").text shouldBe messagesApi("pages.summary.subcontractors.answers.false")
    }

    "have the correct change link for Subcontractors" in {
      document.getElementById(s"subcontractors$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.EmploymentController.subcontractors().toString
    }

    "have the correct question text for First payment" in {
      document.getElementById(s"firstPaymentDate$suffixIdQuestion").text shouldBe messagesApi("pages.summary.firstPaymentDate.question")
    }

    "have the correct answer text for First payment" in {
      document.getElementById(s"firstPaymentDate$suffixIdAnswer").text.matches("([0-9]{1,2}\\/[0-9]{1,2}\\/[0-9]{4})") shouldBe true
    }

    "have the correct change link for First payment" in {
      document.getElementById(s"firstPaymentDate$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.EmploymentController.firstPayment().toString
    }
  }

  "The summary page, Business contact details section" should {
    lazy val summaryModelEmployment = Summary(
      Seq(
        SummarySection(
          id = "businessContactDetails",
          Seq(
            SummaryRow(
              id = "businessEmail",
              answer = Right("test@email.com"),
              changeLink = None
            ),
            SummaryRow(
              id = "mobileNumber",
              answer = Right("1234567890"),
              changeLink = None
            ),
            SummaryRow(
              id = "businessTelephone",
              answer = Right("0987654321"),
              changeLink = None
            )
          )
        )
      )
    )

    lazy val view = summary(summaryModelEmployment)
    lazy val document = Jsoup.parse(view.body)

    "have Business contact details as the summary heading" in {
      document.getElementById(s"businessContactDetails$suffixIdSectionHeading").text shouldBe messagesApi("pages.summary.businessContactDetails.sectionHeading")
    }

    "have the correct question text for business email" in {
      document.getElementById(s"businessEmail$suffixIdQuestion").text shouldBe messagesApi("pages.summary.businessEmail.question")
    }

    "have the correct question text for mobile phone" in {
      document.getElementById(s"mobileNumber$suffixIdQuestion").text shouldBe messagesApi("pages.summary.mobileNumber.question")
    }

    "have the correct question text for business telephone" in {
      document.getElementById(s"businessTelephone$suffixIdQuestion").text shouldBe messagesApi("pages.summary.businessTelephone.question")
    }

    "have the correct answer text for business email" in {
      document.getElementById(s"businessEmail$suffixIdAnswer").text shouldBe "test@email.com"
    }

    "have the correct answer text for mobile number" in {
      document.getElementById(s"mobileNumber$suffixIdAnswer").text shouldBe "1234567890"
    }

    "have the correct answer text for business telephone" in {
      document.getElementById(s"businessTelephone$suffixIdAnswer").text shouldBe "0987654321"
    }
  }

  "The summary page, Nature Of Business section" should {
    lazy val summaryModelDirectorDetails = Summary(
      Seq(
        SummarySection(
          id = "natureOfBusiness",
          Seq(
            SummaryRow(
              id = "natureOfBusiness",
              answer = Right("Flower Arranging"),
              changeLink = Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness()),
              questionArgs = None,
              commonQuestionKey = None
            )
          )
        )
      )
    )

    lazy val view = summary(summaryModelDirectorDetails)
    lazy val document = Jsoup.parse(view.body)

    "have Director details as the summary heading" in {
      document.getElementById(s"natureOfBusiness$suffixIdSectionHeading").text shouldBe messagesApi("pages.summary.natureOfBusiness.sectionHeading")
    }

    "have the correct question text for director0" in {
      document.getElementById(s"natureOfBusiness$suffixIdQuestion").text shouldBe messagesApi("pages.summary.natureOfBusiness.question")
    }

    "have the correct answer text for director0" in {
      document.getElementById(s"natureOfBusiness$suffixIdAnswer").text shouldBe "Flower Arranging"
    }
  }

  "The summary page, Director details section" should {
    lazy val summaryModelDirectorDetails = Summary(
      Seq(
        SummarySection(
          id = "directorDetails",
          Seq(
            SummaryRow(
              id = "director0",
              answer = Right("ZZ123456A"),
              changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
              questionArgs = Some(Seq("Timothy Buttersford")),
              commonQuestionKey = Some("director")
            ),
            SummaryRow(
              id = "director1",
              answer = Right(""),
              changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
              questionArgs = Some(Seq("Pierre Simpson")),
              commonQuestionKey = Some("director")
            )
          )
        )
      )
    )

    lazy val view = summary(summaryModelDirectorDetails)
    lazy val document = Jsoup.parse(view.body)

    "have Director details as the summary heading" in {
      document.getElementById(s"directorDetails$suffixIdSectionHeading").text shouldBe messagesApi("pages.summary.directorDetails.sectionHeading")
    }

    "have the correct question text for director0" in {
      document.getElementById(s"director0$suffixIdQuestion").text shouldBe messagesApi("pages.summary.director.question", "Timothy Buttersford")
    }

    "have the correct question text for director1" in {
      document.getElementById(s"director1$suffixIdQuestion").text shouldBe messagesApi("pages.summary.director.question", "Pierre Simpson")
    }

    "have the correct answer text for director0" in {
      document.getElementById(s"director0$suffixIdAnswer").text shouldBe "ZZ123456A"
    }

    "have the correct answer text for director1" in {
      document.getElementById(s"director1$suffixIdAnswer").text shouldBe ""
    }

    "have the correct change link for director0" in {
      document.getElementById(s"director0$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.DirectorDetailsController.directorDetails().toString
    }

    "have the correct change link for director1" in {
      document.getElementById(s"director1$suffixIdChangeLink").attr("href") shouldBe controllers.userJourney.routes.DirectorDetailsController.directorDetails().toString
    }
  }
}
