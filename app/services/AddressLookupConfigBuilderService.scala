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

package services

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.external._
import play.api.i18n.MessagesApi
import play.api.mvc.Call

@Singleton
class AddressLookupConfigBuilderService @Inject()(implicit messagesApi: MessagesApi, appConfig: AppConfig) {

  lazy val payeRegistrationFrontendURL: String = appConfig.self
  lazy val timeoutLength: Int = appConfig.timeoutInSeconds.toInt

  def buildConfig(handbackLocation: Call, specificJourneyKey: String)(implicit messagesApi: MessagesApi): AlfJourneyConfig = {

    val messageKeyWithSpecKey: String => String = (key: String) => {
      val journeySpecificAlfMessageKey = s"pages.alf.$specificJourneyKey.$key"
      val addressLookupMessageKey = s"pages.alf.common.$key"

      if (messagesApi.isDefinedAt(journeySpecificAlfMessageKey)) journeySpecificAlfMessageKey else addressLookupMessageKey
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
      showPhaseBanner = true,
      alphaPhase = false,
      includeHMRCBranding = false,
      showBackButtons = true,
      deskProServiceName = messagesApi(messageKeyWithSpecKey("deskProServiceName")),
      selectPageConfig = selectPageConfig,
      confirmPageConfig = confirmPageConfig,
      timeoutConfig = timeoutConfig
    )
    val appLevelLabels = AppLevelLabels(
      navTitle = messagesApi(messageKeyWithSpecKey("navTitle")),
      phaseBannerHtml = messagesApi(messageKeyWithSpecKey("phaseBannerHtml"))
    )

    val lookupPageLabels = LookupPageLabels(
      title = messagesApi(messageKeyWithSpecKey("lookupPage.title")),
      heading = messagesApi(messageKeyWithSpecKey("lookupPage.heading")),
      filterLabel = messagesApi(messageKeyWithSpecKey("lookupPage.filterLabel")),
      submitLabel = messagesApi(messageKeyWithSpecKey("lookupPage.submitLabel")),
      manualAddressLinkText = messagesApi(messageKeyWithSpecKey("lookupPage.manual"))
    )

    val selectPageLabels = SelectPageLabels(
      title = messagesApi(messageKeyWithSpecKey("selectPage.description")),
      heading = messagesApi(messageKeyWithSpecKey("selectPage.description")),
      searchAgainLinkText = messagesApi(messageKeyWithSpecKey("selectPage.searchAgain")),
      editAddressLinkText = messagesApi(messageKeyWithSpecKey("selectPage.editAddress"))
    )

    val editPageLabels = EditPageLabels(
      title = messagesApi(messageKeyWithSpecKey("editPage.description")),
      heading = messagesApi(messageKeyWithSpecKey("editPage.description")),
      line1Label = messagesApi(messageKeyWithSpecKey("editPage.line1Label")),
      line2Label = messagesApi(messageKeyWithSpecKey("editPage.line2Label")),
      line3Label = messagesApi(messageKeyWithSpecKey("editPage.line3Label"))
    )

    val confirmPageLabels = ConfirmPageLabels(
      title = messagesApi(messageKeyWithSpecKey("confirmPage.title")),
      heading = messagesApi(messageKeyWithSpecKey("confirmPage.heading")),
      submitLabel = messagesApi(messageKeyWithSpecKey("confirmPage.submitLabel")),
      changeLinkText = messagesApi(messageKeyWithSpecKey("confirmPage.changeLinkText"))
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
