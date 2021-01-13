/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.external._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Call

@Singleton
class AddressLookupConfigBuilderService @Inject()(implicit messagesApi: MessagesApi, appConfig: AppConfig) {

  lazy val payeRegistrationFrontendURL: String = appConfig.self
  lazy val timeoutLength: Int = appConfig.timeoutInSeconds.toInt
  lazy val accessibilityFooterUrl: String = appConfig.accessibilityStatementUrl

  def buildConfig(handbackLocation: Call, specificJourneyKey: String)(implicit messages: Messages): AlfJourneyConfig = {

    val messageKeyWithSpecKey: String => String = (key: String) => {
      val journeySpecificAlfMessageKey = s"pages.alf.$specificJourneyKey.$key"
      val addressLookupMessageKey = s"pages.alf.common.$key"

      if (messages.isDefinedAt(journeySpecificAlfMessageKey)) journeySpecificAlfMessageKey else addressLookupMessageKey
    }

    val selectPageConfig = SelectPageConfig(
      proposalListLimit = 30,
      showSearchAgainLink = true
    )
    val confirmPageConfig = ConfirmPageConfig(
      showSubHeadingAndInfo = false,
      showSearchAgainLink = false,
      showChangeLink = true
    )
    val timeoutConfig = TimeoutConfig(
      timeoutAmount = timeoutLength,
      timeoutUrl = s"$payeRegistrationFrontendURL${controllers.userJourney.routes.SignInOutController.timeoutShow().url}"
    )
    val journeyOptions = JourneyOptions(
      continueUrl = s"$payeRegistrationFrontendURL${handbackLocation.url}",
      homeNavHref = "http://www.hmrc.gov.uk/",
      accessibilityFooterUrl = accessibilityFooterUrl,
      showPhaseBanner = true,
      alphaPhase = false,
      includeHMRCBranding = false,
      showBackButtons = true,
      deskProServiceName = messages(messageKeyWithSpecKey("deskProServiceName")),
      selectPageConfig = selectPageConfig,
      confirmPageConfig = confirmPageConfig,
      timeoutConfig = timeoutConfig,
      disableTranslations = true
    )
    val appLevelLabels = AppLevelLabels(
      navTitle = messages(messageKeyWithSpecKey("navTitle")),
      phaseBannerHtml = messages(messageKeyWithSpecKey("phaseBannerHtml"))
    )

    val lookupPageLabels = LookupPageLabels(
      title = messages(messageKeyWithSpecKey("lookupPage.heading")),
      heading = messages(messageKeyWithSpecKey("lookupPage.heading")),
      filterLabel = messages(messageKeyWithSpecKey("lookupPage.filterLabel")),
      submitLabel = messages(messageKeyWithSpecKey("lookupPage.submitLabel")),
      manualAddressLinkText = messages(messageKeyWithSpecKey("lookupPage.manual"))
    )

    val selectPageLabels = SelectPageLabels(
      title = messages(messageKeyWithSpecKey("selectPage.description")),
      heading = messages(messageKeyWithSpecKey("selectPage.description")),
      searchAgainLinkText = messages(messageKeyWithSpecKey("selectPage.searchAgain")),
      editAddressLinkText = messages(messageKeyWithSpecKey("selectPage.editAddress"))
    )

    val editPageLabels = EditPageLabels(
      title = messages(messageKeyWithSpecKey("editPage.description")),
      heading = messages(messageKeyWithSpecKey("editPage.description")),
      line1Label = messages(messageKeyWithSpecKey("editPage.line1Label")),
      line2Label = messages(messageKeyWithSpecKey("editPage.line2Label")),
      line3Label = messages(messageKeyWithSpecKey("editPage.line3Label"))
    )

    val confirmPageLabels = ConfirmPageLabels(
      title = messages(messageKeyWithSpecKey("confirmPage.heading")),
      heading = messages(messageKeyWithSpecKey("confirmPage.heading")),
      submitLabel = messages(messageKeyWithSpecKey("confirmPage.submitLabel")),
      changeLinkText = messages(messageKeyWithSpecKey("confirmPage.changeLinkText"))
    )

    val journeyLabels = JourneyLabels(
      en = LanguageLabels(
        appLevelLabels,
        selectPageLabels,
        lookupPageLabels,
        editPageLabels,
        confirmPageLabels
      )
    )

    AlfJourneyConfig(
      version = AlfJourneyConfig.defaultConfigVersion,
      options = journeyOptions,
      labels = journeyLabels
    )

  }

}
