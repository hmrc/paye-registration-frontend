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

package views.statements

import helpers.{PayeComponentSpec, PayeFakedApp}
import org.jsoup.Jsoup
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.BaseSelectors
import views.html.pages.statements.accessibility_statement

class AccessibilityStatementViewSpec extends PayeComponentSpec with PayeFakedApp {

  object Selectors extends BaseSelectors

  object Messages {
    val title = "Accessibility statement for registering an employee for PAYE"
    val heading = "Accessibility statement for registering an employee for PAYE"
    val beta = "beta This is a new service - your feedback (opens in new tab) will help us to improve it."
    val hmrc = "HM Revenue & Customs"
    val introP1 = "This accessibility statement explains how accessible this service is, what to do if you have difficulty using it, and how to report accessibility problems with the service."
    val introP2 = "This service is part of the wider GOV.UK website. There is a separate <a href=\"https://www.gov.uk/help/accessibility\">accessibility statement</a> for the main GOV.UK website."

    def introP3(serviceStartUrl: String) = s"This page only contains information about the registering an employee for PAYE service, available at $serviceStartUrl"

    val serviceH2 = "Using this service"
    val serviceP1 = "This service enables businesses to register their employees for PAYE."
    val serviceP2 = "This service is run by HM Revenue and Customs (HMRC). We want as many people as possible to be able to use this service. This means you should be able to:"
    val serviceLi1 = "change colours, contrast levels and fonts"
    val serviceLi2 = "zoom in up to 300% without the text spilling off the screen"
    val serviceLi3 = "get from the start of the service to the end using just a keyboard"
    val serviceLi4 = "get from the start of the service to the end using speech recognition software"
    val serviceLi5 = "listen to the service using a screen reader (including the most recent versions of JAWS, NVDA and VoiceOver)"
    val serviceP3 = "We have also made the text in the service as simple as possible to understand."
    val serviceP4 = "AbilityNet has advice on making your device easier to use if you have a disability."
    val howAccessH2 = "How accessible this service is"
    val howAccessP1 = "This service is partially compliant with the Web Content Accessibility Guidelines version 2.1 AA standard."
    val howAccessP2 = "Some people may find parts of this service difficult to use:"
    val howAccessB1 = "Some of the error messages within the service are repetitive or unclear, however the additional content on the screen should make it easier to understand a mistake."
    val reportProblemH2 = "Reporting accessibility problems with this service"
    val reportProblemP1 = "We are always looking to improve the accessibility of this service. If you find any problems that are not listed on this page or think we are not meeting accessibility requirements, report the accessibility problem."
    val howToDoH2 = "What to do if you are not happy with how we respond to your complaint"
    val whatToDoP1 = "The Equality and Human Rights Commission (EHRC) is responsible for enforcing the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018 (the ’accessibility regulations’). If you are not happy with how we respond to your complaint, contact the Equality Advisory and Support Service (EASS), or the Equality Commission for Northern Ireland (ECNI) if you live in Northern Ireland."
    val contactUsH2 = "Contacting us by phone or getting a visit from us in person"
    val contactUsP1 = "We provide a text relay service if you are deaf, hearing impaired or have a speech impediment."
    val contactUsP2 = "We can provide a British Sign Language (BSL) interpreter, or you can arrange a visit from an HMRC advisor to help you complete the service."
    val contactUsP3 = "Find out how to <a href=\"https://www.gov.uk/dealing-hmrc-additional-needs\">contact us</a>."
    val technicalInfoH2 = "Technical information about this service’s accessibility"
    val technicalInfoP1 = "HMRC is committed to making this service accessible, in accordance with the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018."
    val technicalInfoP2 = "This service is partially compliant with the <a href=\"https://www.w3.org/TR/WCAG21/\">Web Content Accessibility Guidelines version 2.1 AA standard</a> due to the non-compliances listed below."
    val technicalInfoH3 = "Non‐accessible content"
    val technicalInfoH4 = "Non‐compliance with the accessibility regulations"
    val technicalInfoP3 = "The content listed below is non-accessible for the following reasons."
    val technicalInfoP4 = "When an error is committed on the page the error summary contains an unordered list, the list item which contains the ‘Tell us if you’re setting up a new limited company’ has been given an invalid role of ‘tooltip’. This issue may affect how screen reading software reads the list as the list has been structured incorrectly. This will be fixed by 28 January 2022."
    val technicalInfoP5 = "When an error is committed in the ‘National Insurance’ input fields on the page, the error summary links and inline error messages all contain the same information. This may present difficulty to screen reader users who may be unable to distinguish between each error if multiple errors are present in the input fields. The error handling also fails to meet GOV.UK Design System guidelines. This will be fixed by 28 January 2022."
    val howWeTestH2 = "How we tested this service"
    val howWeTestP1 = "The service was last tested on 31 August 2021 and was checked for compliance with WCAG 2.1 AA."
    val howWeTestP2 = "The service was built using parts that were tested by the <a href=\"http://www.digitalaccessibilitycentre.org/\">Digital Accessibility Centre</a>"
    val howWeTestP3 = "This page was prepared on 25 November 2021. It was last updated on 03 December 2021."
    val accessibilityStatementLinkText = "accessibility statement"
    val abilityNetLinkText = "AbilityNet"
    val wcagLinkText = "Web Content Accessibility Guidelines version 2.1 AA standard"
    val contactUsLinkText = "contact us"
    val reportProblemLinkText = "accessibility problem"
    val eassLinkText = "contact the Equality Advisory and Support Service"
    val ecniLinkText = "Equality Commission for Northern Ireland"
    val dacLinkText = "Digital Accessibility Centre"
    val accessibilityLink = "Accessibility"
  }

  val testAccessibilityReportUrl = "testAccessibilityReportUrl"
  val testPageUrl = "testPageUrl"
  val testServiceStartUrl = "testServiceStartUrl"

  implicit val appConfig = mockAppConfig
  implicit val request = FakeRequest()
  implicit lazy val messagesApi: MessagesApi = mockMessagesApi

  lazy val page: Html = {
   val view = app.injector.instanceOf[accessibility_statement]

    view(testAccessibilityReportUrl, testServiceStartUrl)(
      request,
      messagesApi.preferred(request),
      mockAppConfig
    )
  }


  "The accessibility statement page" should {
    lazy val document = Jsoup.parse(page.body)

    "have a heading" in {
      document.select(Selectors.h1).text() mustBe Messages.heading
    }

    "have a title" in {
      document.title must include(Messages.title)
    }

    "have multiple h2" in {
      document.select("h2").eachText() must contain allElementsOf Seq(
        Messages.serviceH2,
        Messages.howAccessH2,
        Messages.reportProblemH2,
        Messages.howToDoH2,
        Messages.contactUsH2,
        Messages.technicalInfoH2,
        Messages.howWeTestH2
      )
    }

    "have multiple h3" in {
      document.select("h3").eachText() must contain allElementsOf Seq(
        Messages.technicalInfoH3,
        Messages.technicalInfoH4,
      )
    }

    "have multiple bullet points" in {
      document.select("li").eachText() must contain allElementsOf Seq(
        Messages.serviceLi1,
        Messages.serviceLi2,
        Messages.serviceLi3,
        Messages.serviceLi4,
        Messages.serviceLi5,
        Messages.howAccessB1
      )
    }
  }
}
