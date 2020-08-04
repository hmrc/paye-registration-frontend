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

import java.util.Locale

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external._
import play.api.i18n.Lang
import play.api.mvc.Call

class AddressLookupConfigBuilderServiceSpec extends PayeComponentSpec with PayeFakedApp {

  implicit val mockMessages = mockMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  object TestService extends AddressLookupConfigBuilderService {
    override lazy val payeRegistrationFrontendURL = "testPayeRegUrl"
    override lazy val timeoutLength = 22666
    override lazy val accessibilityFooterUrl = "http://localhost:9870/register-for-paye/accessibility-statement?pageUri=%2Fregister-for-paye%2F"

  }

  "buildConfig" should {
    "return a filled AlfJourneyConfig model" in {

      val result: AlfJourneyConfig = TestService.buildConfig(
        handbackLocation = Call("GET", "/foo"),
        specificJourneyKey = "ppob"
      )

      val expectedConfig: AlfJourneyConfig = AlfJourneyConfig(
        version = AlfJourneyConfig.defaultConfigVersion,
        options = JourneyOptions(
          continueUrl = "testPayeRegUrl/foo",
          homeNavHref = "http://www.hmrc.gov.uk/",
          accessibilityFooterUrl = "http://localhost:9870/register-for-paye/accessibility-statement?pageUri=%2Fregister-for-paye%2F",
          deskProServiceName = "SCRS",
          showPhaseBanner = true,
          alphaPhase = false,
          showBackButtons = true,
          includeHMRCBranding = false,
          disableTranslations = true,

          selectPageConfig = SelectPageConfig(
            proposalListLimit = 30,
            showSearchAgainLink = true
          ),

          confirmPageConfig = ConfirmPageConfig(
            showSearchAgainLink = false,
            showSubHeadingAndInfo = false,
            showChangeLink = true
          ),

          timeoutConfig = TimeoutConfig(
            timeoutAmount = 22666,
            timeoutUrl = "testPayeRegUrl/register-for-paye/error/timeout"
          )
        ),
        labels = JourneyLabels(en = LanguageLabels(
          appLevelLabels = AppLevelLabels(
            navTitle = "Register an employer for PAYE",
            phaseBannerHtml = "This is a new service. Help us improve it - send your <a href=\"https://www.tax.service.gov.uk/register-for-paye/feedback\">feedback</a>."
          ),
          SelectPageLabels(
            title = "Choose an address",
            heading = "Choose an address",
            searchAgainLinkText = "Search again",
            editAddressLinkText = "Edit address manually"
          ),
          LookupPageLabels(
            title = "Search for your address",
            heading = "Search for your address",
            filterLabel = "House name or number (optional)",
            submitLabel = "Search address",
            manualAddressLinkText = "The address doesn't have a UK postcode"
          ),
          EditPageLabels(
            title = "Enter address",
            heading = "Enter address",
            line1Label = "Address line 1",
            line2Label = "Address line 2",
            line3Label = "Address line 3"
          ),
          ConfirmPageLabels(
            title = "Confirm where you'll carry out most of your business activities",
            heading = "Confirm where you'll carry out most of your business activities",
            submitLabel = "Save and continue",
            changeLinkText = "Change"
          )
        )
        )
      )

      result mustBe expectedConfig

    }
  }

}
