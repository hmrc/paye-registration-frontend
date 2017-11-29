/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import fixtures.BusinessRegistrationFixture
import mocks.MockMetrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import testHelpers.PAYERegSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, NotFoundException}

class AddressLookupConnectorSpec extends PAYERegSpec with BusinessRegistrationFixture {

  val mockMetrics = new MockMetrics

  class Setup {
    val connector = new AddressLookupConnect {
      override val addressLookupFrontendUrl = "testBusinessRegUrl"
      override val payeRegistrationUrl = "testPayeRegistrationUrl"
      override val http = mockWSHttp
      override val metricsService = mockMetrics
      override val successCounter = metricsService.addressLookupSuccessResponseCounter
      override val failedCounter = metricsService.addressLookupFailedResponseCounter
      override def timer = metricsService.addressLookupResponseTimer.time()
      override val timeoutAmount = 900
      override val messagesApi = mockMessages
    }
  }

  implicit val hc = HeaderCarrier()

  val testAddress = Json.obj("x"->"y")

  "getAddress" should {
    "return an address response" in new Setup {
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(testAddress))

      await(connector.getAddress("123")) shouldBe testAddress
    }

    "return a Not Found response" in new Setup {
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      intercept[NotFoundException](await(connector.getAddress("123")))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException](await(connector.getAddress("321")))
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new IndexOutOfBoundsException("other exception")))

      intercept[IndexOutOfBoundsException](await(connector.getAddress("321")))
    }
  }
  "createOnRampJson" should {
    "return a json with timeout and valid continue url" in new Setup{
      val json = Json.parse(
        s"""
          |{
          |  "continueUrl": "${connector.payeRegistrationUrl}/foobar",
          |  "navTitle": "Register your company",
          |  "showPhaseBanner": true,
          |  "phaseBannerHtml": "This is a new service. Help us improve it - send your <a href=\\"https://www.tax.service.gov.uk/register-for-paye/feedback\\">feedback</a>.",
          |  "showBackButtons": true,
          |  "includeHMRCBranding": false,
          |  "deskProServiceName": "SCRS",
          |  "lookupPage": {
          |    "title": "Company address",
          |    "heading": "Search for your address",
          |    "filterLabel": "House name or number (optional)",
          |    "submitLabel": "Search address"
          |  },
          |  "selectPage": {
          |    "title": "Choose an address",
          |    "heading": "Choose an address",
          |    "proposalListLimit": 20,
          |    "showSearchAgainLink": true
          |  },
          |  "editPage": {
          |    "title": "Enter address",
          |    "heading": "Enter address",
          |    "line1Label": "Address line 1",
          |    "line2Label": "Address line 2",
          |    "line3Label": "Address line 3",
          |    "showSearchAgainLink": true
          |  },
          |  "confirmPage": {
          |    "title": "Confirm address",
          |    "heading": "Confirm where you'll carry out most of your business activities",
          |    "showSubHeadingAndInfo": false,
          |    "submitLabel": "Save and continue",
          |    "showChangeLink": true,
          |    "changeLinkText": "Change"
          |  },
          |  "timeout": {
          |    "timeoutAmount": ${connector.timeoutAmount},
          |    "timeoutUrl": "${connector.payeRegistrationUrl}${controllers.userJourney.routes.SignInOutController.destroySession().url}"
          |  }
          |}
        """.stripMargin
      ).as[JsObject]

      connector.createOnRampJson("ppob", Call("GET","/foobar")) shouldBe json
    }

  }
}
