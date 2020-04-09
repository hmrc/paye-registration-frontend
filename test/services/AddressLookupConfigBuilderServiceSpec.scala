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

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external._
import play.api.mvc.Call

class AddressLookupConfigBuilderServiceSpec extends PayeComponentSpec with PayeFakedApp {

  object TestService extends AddressLookupConfigBuilderService(
    frontendAppConfig = mockAppConfig
  ) {
    override lazy val payeRegistrationFrontendURL = "testPayeRegUrl"
    override lazy val timeoutLength = 22666
  }

  "buildConfig" should {
    "return a filled AlfJourneyConfig model" in {

      val result: AlfJourneyConfig = TestService.buildConfig(
        handbackLocation = Call("GET", "/foo"),
        specificJourneyKey = "ppob"
      )

      val expectedConfig: AlfJourneyConfig = AlfJourneyConfig(
        topLevelConfig = TopLevelConfig(
          continueUrl = "testPayeRegUrl/foo",
          homeNavHref = "http://www.hmrc.gov.uk/",
          navTitle = "Register an employer for PAYE",
          showPhaseBanner = true,
          alphaPhase = false,
          phaseBannerHtml = "This is a new service. Help us improve it - send your <a href=\"https://www.tax.service.gov.uk/register-for-paye/feedback\">feedback</a>.",
          includeHMRCBranding = false,
          showBackButtons = true,
          deskProServiceName = "SCRS"
        ),
        lookupPageConfig = LookupPageConfig(
          title = "Company address",
          heading = "Search for your address",
          filterLabel = "House name or number (optional)",
          submitLabel = "Search address",
          manualAddressLinkText = "The address doesn't have a UK postcode"
        ),
        selectPageConfig = SelectPageConfig(
          title = "Choose an address",
          heading = "Choose an address",
          proposalListLimit = 20,
          showSearchAgainLink = true,
          searchAgainLinkText = "Search again",
          editAddressLinkText = "Edit address manually"
        ),
        editPageConfig = EditPageConfig(
          title = "Enter address",
          heading = "Enter address",
          line1Label = "Address line 1",
          line2Label = "Address line 2",
          line3Label = "Address line 3",
          showSearchAgainLink = true
        ),
        confirmPageConfig = ConfirmPageConfig(
          title = "Confirm address",
          heading = "Confirm where you'll carry out most of your business activities",
          showSubHeadingAndInfo = false,
          submitLabel = "Save and continue",
          showSearchAgainLink = false,
          showChangeLink = true,
          changeLinkText = "Change"
        ),
        timeoutConfig = TimeoutConfig(
          timeoutAmount = 22666,
          timeoutUrl = "testPayeRegUrl/register-for-paye/error/timeout"
        )
      )

      result mustBe expectedConfig

    }
  }

}
