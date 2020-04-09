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

case class AlfJourneyConfig(topLevelConfig: TopLevelConfig,
                            timeoutConfig: TimeoutConfig,
                            lookupPageConfig: LookupPageConfig,
                            selectPageConfig: SelectPageConfig,
                            editPageConfig: EditPageConfig,
                            confirmPageConfig: ConfirmPageConfig)

object AlfJourneyConfig {
  implicit val writer: OWrites[AlfJourneyConfig] = new OWrites[AlfJourneyConfig] {
    override def writes(alfJourneyConfig: AlfJourneyConfig): JsObject = {
      Json.toJson(alfJourneyConfig.topLevelConfig).as[JsObject] ++ Json.obj(
        "timeout" -> Json.toJson(alfJourneyConfig.timeoutConfig),
        "lookupPage" -> Json.toJson(alfJourneyConfig.lookupPageConfig),
        "selectPage" -> Json.toJson(alfJourneyConfig.selectPageConfig),
        "editPage" -> Json.toJson(alfJourneyConfig.editPageConfig),
        "confirmPage" -> Json.toJson(alfJourneyConfig.confirmPageConfig)
      )
    }
  }

  implicit val reader: Reads[AlfJourneyConfig] = new Reads[AlfJourneyConfig] {
    override def reads(json: JsValue): JsResult[AlfJourneyConfig] = {

      val continueUrl = (json \ "continueUrl").as[String]
      val homeNavHref = (json \ "homeNavHref").as[String]
      val navTitle = (json \ "navTitle").as[String]
      val showPhaseBanner = (json \ "showPhaseBanner").as[Boolean]
      val alphaPhaseBanner = (json \ "alphaPhaseBanner").as[Boolean]
      val phaseBannerHtml = (json \ "phaseBannerHtml").as[String]
      val includeHMRCBranding = (json \ "includeHMRCBranding").as[Boolean]
      val showBackButtons = (json \ "showBackButtons").as[Boolean]
      val deskProServiceName = (json \ "deskProServiceName").as[String]

      val topLevelConfig = TopLevelConfig(
        continueUrl,
        homeNavHref,
        navTitle,
        showPhaseBanner,
        alphaPhaseBanner,
        phaseBannerHtml,
        includeHMRCBranding,
        showBackButtons,
        deskProServiceName
      )

      val timeoutConfig = (json \ "timeout").as[TimeoutConfig]
      val lookupPageConfig = (json \ "lookupPage").as[LookupPageConfig]
      val selectPageConfig = (json \ "selectPage").as[SelectPageConfig]
      val editPageConfig = (json \ "editPage").as[EditPageConfig]
      val confirmPageConfig = (json \ "confirmPage").as[ConfirmPageConfig]

      JsSuccess(
        AlfJourneyConfig(
          topLevelConfig,
          timeoutConfig,
          lookupPageConfig,
          selectPageConfig,
          editPageConfig,
          confirmPageConfig
        )
      )

    }
  }

}

case class TopLevelConfig(continueUrl: String,
                          homeNavHref: String,
                          navTitle: String,
                          showPhaseBanner: Boolean,
                          alphaPhase: Boolean,
                          phaseBannerHtml: String,
                          includeHMRCBranding: Boolean,
                          showBackButtons: Boolean,
                          deskProServiceName: String)

object TopLevelConfig {
  implicit val format: OFormat[TopLevelConfig] = Json.format[TopLevelConfig]
}

case class TimeoutConfig(timeoutAmount: Int,
                         timeoutUrl: String)

object TimeoutConfig {
  implicit val format: OFormat[TimeoutConfig] = Json.format[TimeoutConfig]
}

case class LookupPageConfig(title: String,
                            heading: String,
                            filterLabel: String,
                            submitLabel: String,
                            manualAddressLinkText: String)

object LookupPageConfig {
  implicit val format: OFormat[LookupPageConfig] = Json.format[LookupPageConfig]
}

case class SelectPageConfig(title: String,
                            heading: String,
                            proposalListLimit: Int,
                            showSearchAgainLink: Boolean,
                            searchAgainLinkText: String,
                            editAddressLinkText: String)

object SelectPageConfig {
  implicit val format: OFormat[SelectPageConfig] = Json.format[SelectPageConfig]
}

case class EditPageConfig(title: String,
                          heading: String,
                          line1Label: String,
                          line2Label: String,
                          line3Label: String,
                          showSearchAgainLink: Boolean)

object EditPageConfig {
  implicit val format: OFormat[EditPageConfig] = Json.format[EditPageConfig]
}

case class ConfirmPageConfig(title: String,
                             heading: String,
                             showSubHeadingAndInfo: Boolean,
                             submitLabel: String,
                             showSearchAgainLink: Boolean,
                             showChangeLink: Boolean,
                             changeLinkText: String)

object ConfirmPageConfig {
  implicit val format: OFormat[ConfirmPageConfig] = Json.format[ConfirmPageConfig]
}