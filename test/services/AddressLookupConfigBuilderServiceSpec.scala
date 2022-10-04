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

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external._
import play.api.i18n.Lang
import play.api.mvc.Call

import java.util.Locale

class AddressLookupConfigBuilderServiceSpec extends PayeComponentSpec with PayeFakedApp {

  implicit val mockMessages = mockMessagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  object TestService extends AddressLookupConfigBuilderService(mockAppConfig, mockMessagesApi) {
    override lazy val payeRegistrationFrontendURL = "testPayeRegUrl"
    override lazy val timeoutLength = 22666
    override lazy val accessibilityFooterUrl = "http://localhost:9870/register-for-paye/accessibility-statement?pageUri=%2Fregister-for-paye%2F"

  }

  "buildConfig" should {
    "return a filled AlfJourneyConfig model" in {
      System.setProperty("feature.isWelsh", "true")

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
          disableTranslations = false,

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
            navTitle = Some("Register an employer for PAYE"),
            phaseBannerHtml = Some("This is a new service. Help us improve it - send your <a href=\"https://www.tax.service.gov.uk/register-for-paye/feedback\">feedback</a>.")
          ),
          SelectPageLabels(
            title = Some("Choose an address"),
            heading = Some("Choose an address"),
            searchAgainLinkText = Some("Search again"),
            editAddressLinkText = Some("Edit address manually")
          ),
          LookupPageLabels(
            title = Some("Search for your address"),
            heading = Some("Search for your address"),
            filterLabel = Some("House name or number (optional)"),
            submitLabel = Some("Search address"),
            manualAddressLinkText = Some("The address doesn’t have a UK postcode")
          ),
          EditPageLabels(
            title = Some("Enter address"),
            heading = Some("Enter address"),
            line1Label = Some("Address line 1"),
            line2Label = Some("Address line 2"),
            line3Label = Some("Address line 3")
          ),
          ConfirmPageLabels(
            title = Some("Review and confirm your address"),
            heading = Some("Review and confirm your address"),
            submitLabel = Some("Save and continue"),
            changeLinkText = Some("Change")
          )
        ),
          cy = LanguageLabels(
            appLevelLabels = AppLevelLabels(
              navTitle = Some("Cofrestru cyflogwr ar gyfer TWE"),
              phaseBannerHtml = Some("""Mae hwn yn wasanaeth newydd. Helpwch ni i’w wella – anfonwch eich <a href="https://www.tax.service.gov.uk/register-for-paye/feedback">adborth</a>.""")
            ),
            SelectPageLabels(
              title = Some("Dewiswch gyfeiriad"),
              heading = Some("Dewiswch gyfeiriad"),
              searchAgainLinkText = Some("Chwilio eto"),
              editAddressLinkText = Some("Golygwch y cyfeiriad â llaw")
            ),
            LookupPageLabels(
              title = Some("Chwiliwch am eich cyfeiriad"),
              heading = Some("Chwiliwch am eich cyfeiriad"),
              filterLabel = Some("Enw neu rif y tŷ (dewisol)"),
              submitLabel = Some("Chwilio am y cyfeiriad"),
              manualAddressLinkText = Some("Nid oes gan y cyfeiriad god post yn y DU")
            ),
            EditPageLabels(
              title = Some("Nodwch gyfeiriad"),
              heading = Some("Nodwch gyfeiriad"),
              line1Label = Some("Cyfeiriad - llinell 1"),
              line2Label = Some("Cyfeiriad - llinell 2"),
              line3Label = Some("Cyfeiriad - llinell 3")
            ),
            ConfirmPageLabels(
              title = Some("Adolygu a chadarnhau’ch cyfeiriad"),
              heading = Some("Adolygu a chadarnhau’ch cyfeiriad"),
              submitLabel = Some("Cadw ac yn eich blaen"),
              changeLinkText = Some("Newid")
            )
          )
        )
      )

      result mustBe expectedConfig

    }
  }

}
