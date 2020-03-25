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

package models.view

import play.api.mvc.Call

case class Summary(sections: Seq[SummarySection])

case class SummarySection(id: String, sectionHeading: String, rows: Seq[SummaryRow])

case class SummaryRow(id: String,
                      question: String,
                      answers: Seq[String],
                      optChangeLink: Option[SummaryChangeLink])

case class SummaryChangeLink(location: Call, hiddenText: String)