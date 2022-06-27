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

package models

import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, _}
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}

object SummaryListRowUtils {
  def optSummaryListRowString(questionId: String,
                              optAnswer: Option[String],
                              optUrl: Option[String],
                              questionArgs: Seq[String] = Nil
                             )(implicit messages: Messages): Option[SummaryListRow] = {
    optSummaryListRow(
      questionId,
      optAnswer.map(answer => HtmlContent(messages(answer))),
      optUrl,
      questionArgs
    )
  }

  def optSummaryListRowBoolean(questionId: String,
                               optAnswer: Option[Boolean],
                               optUrl: Option[String],
                               questionArgs: Seq[String] = Nil
                              )(implicit messages: Messages): Option[SummaryListRow] =
    optSummaryListRow(
      questionId,
      optAnswer.map {
        case true => HtmlContent(messages("app.common.yes"))
        case false => HtmlContent(messages("app.common.no"))
      },
      optUrl,
      questionArgs
    )

  def optSummaryListRowSeq(questionId: String,
                           optAnswers: Option[Seq[String]],
                           optUrl: Option[String],
                           questionArgs: Seq[String] = Nil
                          )(implicit messages: Messages): Option[SummaryListRow] =
    optSummaryListRow(
      questionId,
      optAnswers.map(answers => HtmlContent(answers.map(messages(_)).mkString("<br>"))),
      optUrl,
      questionArgs
    )

  def optSummaryListRowMessageSeq(questionId: Option[Seq[String]],
                           optAnswers: Option[Seq[String]],
                           optUrl: Option[String],
                           questionArgs: Seq[String] = Nil
                          )(implicit messages: Messages): Option[SummaryListRow] =
    optSummaryListMessageRow(
      questionId.map(answers => HtmlContent(answers.map(messages(_)).mkString("<br>"))),
      optAnswers.map(answers => HtmlContent(answers.map(messages(_)).mkString("<br>"))),
      optUrl,
      questionArgs
    )

  private def optSummaryListRow(questionId: String,
                                optAnswer: Option[HtmlContent],
                                optUrl: Option[String],
                                questionArgs: Seq[String] = Nil
                               )(implicit messages: Messages): Option[SummaryListRow] =
    optAnswer.map { answer =>
      SummaryListRow(
        key = Key(Text(messages(questionId, questionArgs: _*)), "govuk-!-width-one-half"),
        value = Value(answer),
        actions = optUrl.map { url =>
          Actions(
            items = Seq(
              ActionItem(
                href = url,
                content = Text(messages("app.common.change")),
                visuallyHiddenText = Some(messages(questionId, questionArgs: _*))
              )
            )
          )
        }
      )
    }


  private def optSummaryListMessageRow(questionId: Option[HtmlContent],
                                optAnswer: Option[HtmlContent],
                                optUrl: Option[String],
                                questionArgs: Seq[String] = Nil
                               )(implicit messages: Messages): Option[SummaryListRow] =
    optAnswer.map { answer =>
      SummaryListRow(
        key = Key(answer),
        value = Value(answer),
        actions = optUrl.map { url =>
          Actions(
            items = Seq(
              ActionItem(
                href = url,
                content = Text(messages("app.common.change"))
              )
            )
          )
        }
      )
    }
}
