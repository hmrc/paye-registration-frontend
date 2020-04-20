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

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import models.external._
import play.api.i18n.MessagesApi
import play.api.mvc.Call

@Singleton
class AddressLookupConfigBuilderService @Inject()(frontendAppConfig: FrontendAppConfig
                                                 )(implicit messagesApi: MessagesApi) {

  lazy val payeRegistrationFrontendURL: String = frontendAppConfig.self
  lazy val timeoutLength: Int = frontendAppConfig.timeoutInSeconds.toInt

  def buildConfig(handbackLocation: Call, specificJourneyKey: String)(implicit messagesApi: MessagesApi): AlfJourneyConfig = {

    val messageKeyWithSpecKey: String => String = (key: String) => {
      val journeySpecificAlfMessageKey = s"pages.alf.$specificJourneyKey.$key"
      val addressLookupMessageKey = s"pages.alf.common.$key"

      if (messagesApi.isDefinedAt(journeySpecificAlfMessageKey)) journeySpecificAlfMessageKey else addressLookupMessageKey
    }

    val topLevelConfig = TopLevelConfig(
      continueUrl = s"$payeRegistrationFrontendURL${handbackLocation.url}",
      homeNavHref = "http://www.hmrc.gov.uk/",
      navTitle = messagesApi(messageKeyWithSpecKey("navTitle")),
      showPhaseBanner = true,
      alphaPhase = false,
      phaseBannerHtml = messagesApi(messageKeyWithSpecKey("phaseBannerHtml")),
      includeHMRCBranding = false,
      showBackButtons = true,
      deskProServiceName = messagesApi(messageKeyWithSpecKey("deskProServiceName"))
    )

    val timeoutConfig = TimeoutConfig(
      timeoutAmount = timeoutLength,
      timeoutUrl = s"$payeRegistrationFrontendURL${controllers.userJourney.routes.SignInOutController.timeoutShow().url}"
    )

    val lookupPageConfig = LookupPageConfig(
      title = messagesApi(messageKeyWithSpecKey("lookupPage.title")),
      heading = messagesApi(messageKeyWithSpecKey("lookupPage.heading")),
      filterLabel = messagesApi(messageKeyWithSpecKey("lookupPage.filterLabel")),
      submitLabel = messagesApi(messageKeyWithSpecKey("lookupPage.submitLabel")),
      manualAddressLinkText = messagesApi(messageKeyWithSpecKey("lookupPage.manual"))
    )

    val selectPageConfig = SelectPageConfig(
      title = messagesApi(messageKeyWithSpecKey("selectPage.description")),
      heading = messagesApi(messageKeyWithSpecKey("selectPage.description")),
      proposalListLimit = 20,
      showSearchAgainLink = true,
      searchAgainLinkText = messagesApi(messageKeyWithSpecKey("selectPage.searchAgain")),
      editAddressLinkText = messagesApi(messageKeyWithSpecKey("selectPage.editAddress"))
    )

    val editPageConfig = EditPageConfig(
      title = messagesApi(messageKeyWithSpecKey("editPage.description")),
      heading = messagesApi(messageKeyWithSpecKey("editPage.description")),
      line1Label = messagesApi(messageKeyWithSpecKey("editPage.line1Label")),
      line2Label = messagesApi(messageKeyWithSpecKey("editPage.line2Label")),
      line3Label = messagesApi(messageKeyWithSpecKey("editPage.line3Label")),
      showSearchAgainLink = true
    )

    val confirmPageConfig = ConfirmPageConfig(
      title = messagesApi(messageKeyWithSpecKey("confirmPage.title")),
      heading = messagesApi(messageKeyWithSpecKey("confirmPage.heading")),
      showSubHeadingAndInfo = false,
      submitLabel = messagesApi(messageKeyWithSpecKey("confirmPage.submitLabel")),
      showSearchAgainLink = false,
      showChangeLink = true,
      changeLinkText = messagesApi(messageKeyWithSpecKey("confirmPage.changeLinkText"))
    )

    AlfJourneyConfig(
      topLevelConfig = topLevelConfig,
      timeoutConfig = timeoutConfig,
      lookupPageConfig = lookupPageConfig,
      selectPageConfig = selectPageConfig,
      editPageConfig = editPageConfig,
      confirmPageConfig = confirmPageConfig
    )

  }

}
