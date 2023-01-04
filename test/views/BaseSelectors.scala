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

package views

trait BaseSelectors {

  val panelHeading = "main div.govuk-panel.govuk-panel--confirmation h1"
  val panelBody = "main div.govuk-panel.govuk-panel--confirmation div.govuk-panel__body"
  val h1: String = "h1"
  val title: String = "title"
  val backLink: String = "back-link"
  def h2(n: Int) = s"main h2:nth-of-type($n)"
  val h2: String = "h2"
  def h2ConfirmationPage(n: Int) = s"#main-content > div > div > h2:nth-of-type($n)"
  val p: Int => String = i => s"main p:nth-of-type($i)"
  val legend: Int => String = i => s"main div div div:nth-of-type($i) fieldset legend"
  val link: Int => String = i => s"main a:nth-of-type($i)"
  val indent = "div.govuk-inset-text"
  val hint = "main div.govuk-hint"
  val bullet: Int => String = i => s"main ul.govuk-list.govuk-list--bullet li:nth-of-type($i)"
  val label = "main label.govuk-label"
  val nthLabel: Int => String = i => s"form > div > div:nth-child($i) > label"
  val warning = "main .govuk-warning-text__text"
  val contactHmrc = "#contact-hmrc"
  val button = ".govuk-button"
  val detailsSummary = ".govuk-details__summary-text"
  val detailsContent = ".govuk-details__text"
  val radioYes = "label[for=value]"
  val radioNo = "label[for=value-no]"
  def radio(n: Int) = s"div.govuk-radios__item:nth-of-type($n) label"

}
