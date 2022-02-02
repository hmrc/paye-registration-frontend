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

package views.pages

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.{Summary, SummaryChangeLink, SummaryRow, SummarySection}
import org.jsoup.Jsoup
import org.jsoup.parser.Tag
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html.pages.summary

import java.util.Locale

class SummarySpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {
  implicit val appConfig = mockAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = mockMessagesApi
  implicit val mockMessages = mockMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  val suffixIdSectionHeading = "SectionHeading"
  val suffixIdSectionTable = "Table"
  val suffixIdQuestion = "Question"
  val suffixIdAnswer = "Answer"
  val suffixIdChangeLink = "ChangeLink"
  val suffixIdHelpText = "HelpText"

  "The summary page" should {
    val testSummarySectionId1 = "testSummarySectionId1"
    val testSectionHeading1 = "testSectionHeading1"

    val testSummaryRowId1 = "testSummaryRowId1"
    val testQuestion1 = "testQuestion1"
    val testAnswer1A = "testAnswer1A"
    val testAnswer1B = "testAnswer1B"
    val testHiddenChangeText1 = "testHiddenChangeText1"
    val testChangeLink1 = Call("testMethod1", "testUrl1")

    val testSummaryRow1 = SummaryRow(
      testSummaryRowId1,
      testQuestion1,
      Seq(
        testAnswer1A,
        testAnswer1B
      ),
      optChangeLink = Some(SummaryChangeLink(
        testChangeLink1,
        testHiddenChangeText1
      ))
    )

    val testSummaryRowId2 = "testSummaryRowId2"
    val testQuestion2 = "testQuestion2"
    val testAnswer2A = "testAnswer2A"
    val testHiddenChangeText2 = "testHiddenChangeText2"
    val testChangeLink2 = Call("testMethod2", "testUrl2")

    val testSummaryRow2 = SummaryRow(
      testSummaryRowId2,
      testQuestion2,
      Seq(
        testAnswer2A
      ),
      optChangeLink = Some(SummaryChangeLink(
        testChangeLink2,
        testHiddenChangeText2
      ))
    )

    val testSummarySectionId2 = "testSummarySectionId2"
    val testSectionHeading2 = "testSectionHeading2"

    val testSummaryRowId3 = "testSummaryRowId3"
    val testQuestion3 = "testQuestion3"
    val testAnswer3A = "testAnswer3A"

    val testSummaryRow3 = SummaryRow(
      testSummaryRowId3,
      testQuestion3,
      Seq(
        testAnswer3A
      ),
      optChangeLink = None
    )

    lazy val testSummaryModel = Summary(
      Seq(
        SummarySection(
          testSummarySectionId1,
          testSectionHeading1,
          Seq(
            testSummaryRow1,
            testSummaryRow2
          )
        ),
        SummarySection(
          testSummarySectionId2,
          testSectionHeading2,
          Seq(
            testSummaryRow3
          )
        )
      )
    )


    lazy val view = app.injector.instanceOf[summary]
    lazy val document = Jsoup.parse(view(testSummaryModel).body)

    "have the correct title" in {
      document.getElementById("pageHeading").text mustBe mockMessages("pages.summary.heading")
    }

    for {summarySection <- testSummaryModel.sections} {
      s"have the section ${summarySection.id} which" should {
        "display section headers correctly" in {
          document.getElementById(s"${summarySection.id}$suffixIdSectionHeading").text mustBe summarySection.sectionHeading
        }

        "contain a section table" in {
          document.getElementById(s"${summarySection.id}$suffixIdSectionTable").tag mustBe Tag.valueOf("dl")
        }
      }
      for {summaryRow <- summarySection.rows} {
        s"have the summary row ${summaryRow.id} which" should {
          s"display the question ${summaryRow.question}" in {
            document.getElementById(s"${summaryRow.id}$suffixIdQuestion").text mustBe summaryRow.question
          }
          "the answers" should {
            "be wrapped in a table cell" in {
              document.getElementById(s"${summaryRow.id}$suffixIdAnswer").tag mustBe Tag.valueOf("dd")
            }
            for {answer <- summaryRow.answers} {
              s"display the answer $answer" in {
                document.getElementById(s"${summaryRow.id}$suffixIdAnswer").text must include(answer)
              }
            }
          }
        }
        summaryRow.optChangeLink match {
          case Some(changeLink) =>
            "have a change link which" should {
              s"have the correct url - ${changeLink.location.url}" in {
                document.getElementById(s"${summaryRow.id}$suffixIdChangeLink").attr("href") mustBe changeLink.location.url
              }
              s"have the correct hidden text - ${changeLink.hiddenText}" in {
                document.getElementById(s"${summaryRow.id}$suffixIdHelpText").text mustBe changeLink.hiddenText
              }
            }
          case None =>
            "not have a change link" in {
              document.getElementById(s"${summaryRow.id}$suffixIdChangeLink") mustBe null
            }
        }

      }

    }
  }
}
