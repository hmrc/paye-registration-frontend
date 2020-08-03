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

import play.api.libs.json._

case class AlfJourneyConfig(version: Int = AlfJourneyConfig.defaultConfigVersion,
                            options: JourneyOptions,
                            labels: JourneyLabels
                           )

case class JourneyOptions(continueUrl: String,
                          homeNavHref: String,
                          accessibilityFooterUrl: String,
                          deskProServiceName: String,
                          showPhaseBanner: Boolean,
                          alphaPhase: Boolean,
                          showBackButtons: Boolean,
                          includeHMRCBranding: Boolean,
                          selectPageConfig: SelectPageConfig,
                          confirmPageConfig: ConfirmPageConfig,
                          timeoutConfig: TimeoutConfig,
                          disableTranslations: Boolean
                         )

case class SelectPageConfig(proposalListLimit: Int,
                            showSearchAgainLink: Boolean
                           )

case class ConfirmPageConfig(showSearchAgainLink: Boolean,
                             showSubHeadingAndInfo: Boolean,
                             showChangeLink: Boolean
                            )

case class TimeoutConfig(timeoutAmount: Int,
                         timeoutUrl: String
                        )

case class JourneyLabels(en: LanguageLabels
                        )

case class LanguageLabels(appLevelLabels: AppLevelLabels,
                          selectPageLabels: SelectPageLabels,
                          lookupPageLabels: LookupPageLabels,
                          editPageLabels: EditPageLabels,
                          confirmPageLabels: ConfirmPageLabels
                         )

case class AppLevelLabels(navTitle: String,
                          phaseBannerHtml: String
                         )

case class SelectPageLabels(title: String,
                            heading: String,
                            searchAgainLinkText: String,
                            editAddressLinkText: String
                           )

case class LookupPageLabels(title: String,
                            heading: String,
                            filterLabel: String,
                            submitLabel: String,
                            manualAddressLinkText: String
                           )

case class EditPageLabels(title: String,
                          heading: String,
                          line1Label: String,
                          line2Label: String,
                          line3Label: String
                         )

case class ConfirmPageLabels(title: String,
                             heading: String,
                             submitLabel: String,
                             changeLinkText: String
                            )

object AlfJourneyConfig {
  val defaultConfigVersion = 2

  implicit lazy val journeyConfigFormat: OFormat[AlfJourneyConfig] = Json.format[AlfJourneyConfig]
  implicit lazy val journeyOptionsFormat: OFormat[JourneyOptions] = Json.format[JourneyOptions]
  implicit lazy val selectPageConfigFormat: OFormat[SelectPageConfig] = Json.format[SelectPageConfig]
  implicit lazy val confirmPageConfigFormat: OFormat[ConfirmPageConfig] = Json.format[ConfirmPageConfig]
  implicit lazy val timeoutConfigFormat: OFormat[TimeoutConfig] = Json.format[TimeoutConfig]
  implicit lazy val journeyLabelsFormat: OFormat[JourneyLabels] = Json.format[JourneyLabels]
  implicit lazy val languageLabelsFormat: OFormat[LanguageLabels] = Json.format[LanguageLabels]
  implicit lazy val appLevelLabelsFormat: OFormat[AppLevelLabels] = Json.format[AppLevelLabels]
  implicit lazy val selectPageLabelsFormat: OFormat[SelectPageLabels] = Json.format[SelectPageLabels]
  implicit lazy val lookupPageLabelsFormat: OFormat[LookupPageLabels] = Json.format[LookupPageLabels]
  implicit lazy val editPageLabelsFormat: OFormat[EditPageLabels] = Json.format[EditPageLabels]
  implicit lazy val confirmPageLabelsFormat: OFormat[ConfirmPageLabels] = Json.format[ConfirmPageLabels]
}