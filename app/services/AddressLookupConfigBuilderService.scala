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

package services

import config.AppConfig
import models.external._
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Call
import utils.MessageOption

import javax.inject.{Inject, Singleton}

@Singleton
class AddressLookupConfigBuilderService @Inject()(appConfig: AppConfig, val messagesApi: MessagesApi) {

  val english = Lang("en")
  val welsh = Lang("cy")

  lazy val payeRegistrationFrontendURL: String = appConfig.self
  lazy val timeoutLength: Int = appConfig.timeoutInSeconds
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
      timeoutUrl = s"$payeRegistrationFrontendURL${controllers.userJourney.routes.SignInOutController.timeoutShow.url}"
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
      disableTranslations = !appConfig.languageTranslationEnabled
    )

    def appLevelLabels(lang: Lang) = {
      AppLevelLabels(
        navTitle = MessageOption("pages.alf.common.navTitle", lang)(messagesApi),
        phaseBannerHtml = MessageOption("pages.alf.common.phaseBannerHtml", lang)(messagesApi)
      )
    }

    def lookupPageLabels(lang: Lang) = {
      LookupPageLabels(
        title = MessageOption("pages.alf.common.lookupPage.heading", lang)(messagesApi),
        heading = MessageOption("pages.alf.common.lookupPage.heading", lang)(messagesApi),
        filterLabel = MessageOption("pages.alf.common.lookupPage.filterLabel", lang)(messagesApi),
        submitLabel = MessageOption("pages.alf.common.lookupPage.submitLabel", lang)(messagesApi),
        manualAddressLinkText = MessageOption("pages.alf.common.lookupPage.manual", lang)(messagesApi)
      )
    }

    def selectPageLabels(lang: Lang) = SelectPageLabels(
      title = MessageOption("pages.alf.common.selectPage.description", lang)(messagesApi),
      heading = MessageOption("pages.alf.common.selectPage.description", lang)(messagesApi),
      searchAgainLinkText = MessageOption("pages.alf.common.selectPage.searchAgain", lang)(messagesApi),
      editAddressLinkText = MessageOption("pages.alf.common.selectPage.editAddress", lang)(messagesApi)
    )

    def editPageLabels(lang: Lang) = EditPageLabels(
      title = MessageOption("pages.alf.common.editPage.description", lang)(messagesApi),
      heading = MessageOption("pages.alf.common.editPage.description", lang)(messagesApi),
      line1Label = MessageOption("pages.alf.common.editPage.line1Label", lang)(messagesApi),
      line2Label = MessageOption("pages.alf.common.editPage.line2Label", lang)(messagesApi),
      line3Label = MessageOption("pages.alf.common.editPage.line3Label", lang)(messagesApi)
    )

    def confirmPageLabels(lang: Lang) = ConfirmPageLabels(
      title = MessageOption("pages.alf.common.confirmPage.heading", lang)(messagesApi),
      heading = MessageOption("pages.alf.common.confirmPage.heading", lang)(messagesApi),
      submitLabel = MessageOption("pages.alf.common.confirmPage.submitLabel", lang)(messagesApi),
      changeLinkText = MessageOption("pages.alf.common.confirmPage.changeLinkText", lang)(messagesApi)
    )

    val journeyLabels = JourneyLabels(
      en = LanguageLabels(
        appLevelLabels(english),
        selectPageLabels(english),
        lookupPageLabels(english),
        editPageLabels(english),
        confirmPageLabels(english)
      ),
      cy = LanguageLabels(
        appLevelLabels(welsh),
        selectPageLabels(welsh),
        lookupPageLabels(welsh),
        editPageLabels(welsh),
        confirmPageLabels(welsh)
      )
    )

    AlfJourneyConfig(
      version = AlfJourneyConfig.defaultConfigVersion,
      options = journeyOptions,
      labels = journeyLabels
    )

  }

}
