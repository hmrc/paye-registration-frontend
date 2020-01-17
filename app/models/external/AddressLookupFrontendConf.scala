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

package models.external

import play.api.libs.json.Json

case class LookupPage(title: String,
                      heading: String,
                      filterLabel: String,
                      submitLabel: String)

object LookupPage {
  implicit val format = Json.format[LookupPage]
}

case class SelectPage(title: String,
                      heading: String,
                      proposalListLimit: Int,
                      showSearchAgainLink: Boolean)

object SelectPage {
  implicit val format = Json.format[SelectPage]
}

case class EditPage(title: String,
                    heading: String,
                    line1Label: String,
                    line2Label: String,
                    line3Label: String,
                    showSearchAgainLink: Boolean)

object EditPage {
  implicit val format = Json.format[EditPage]
}

case class ConfirmPage(title: String,
                       heading: String,
                       showSubHeadingAndInfo: Boolean,
                       submitLabel: String,
                       showChangeLink: Boolean,
                       changeLinkText: String)

object ConfirmPage {
  implicit val format = Json.format[ConfirmPage]
}

case class Timeout(timeoutAmount: Int,
                   timeoutUrl: String)

object Timeout {
  implicit val format = Json.format[Timeout]
}

case class AddressLookupFrontendConf(continueUrl: String,
                                     navTitle: String,
                                     showPhaseBanner: Boolean,
                                     phaseBannerHtml: String,
                                     showBackButtons: Boolean,
                                     includeHMRCBranding: Boolean,
                                     deskProServiceName: String,
                                     lookupPage: LookupPage,
                                     selectPage: SelectPage,
                                     editPage: EditPage,
                                     confirmPage: ConfirmPage,
                                     timeout: Timeout)

object AddressLookupFrontendConf {
  implicit val format = Json.format[AddressLookupFrontendConf]
}
