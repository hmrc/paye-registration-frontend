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

package controllers.userJourney

import builders.AuthBuilder
import common.exceptions.DownstreamExceptions.CompanyDetailsNotFoundException
import enums.CacheKeys
import fixtures.CoHoAPIFixture
import helpers.PAYERegSpec
import models.coHo.CoHoCompanyDetailsModel
import models.formModels.TradingNameFormModel
import models.payeRegistration.companyDetails.TradingName
import org.jsoup._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.test.FakeRequest
import services.S4LService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class CompanyDetailsControllerSpec extends PAYERegSpec with CoHoAPIFixture {

  val mockS4LService = mock[S4LService]

  class Setup {
    val controller = new CompanyDetailsController {
      override val s4LService = mockS4LService
      override val keystoreConnector = mockKeystoreConnector
      override val authConnector = mockAuthConnector
    }
  }

  val tstTradingNameFormModel = TradingNameFormModel(tradeUnderDifferentName = "yes", tradingName = Some("test trading name"))
  val tstTradingNameDataModel = TradingName(Some("test trading name"))

  val fakeRequest = FakeRequest("GET", "/")
  implicit val materializer = fakeApplication.materializer

  "calling the tradingName action" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.tradingName()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "show the correctly pre-populated trading name page when data has already been entered" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, Some(validCoHoCompanyDetailsResponse))
      when(mockS4LService.fetchAndGet[TradingName](Matchers.matches(CacheKeys.TradingName.toString))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(tstTradingNameDataModel)))

      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        response =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body().getElementById("pageHeading").text() shouldBe s"Does the company trade under any other names than ${validCoHoCompanyDetailsResponse.companyName}?"
          result.body().getElementById("tradingName").attr("value") shouldBe "test trading name"
      }
    }

    "show the a blank trading name page when no data has been entered" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, Some(validCoHoCompanyDetailsResponse))
      when(mockS4LService.fetchAndGet[TradingName](Matchers.matches(CacheKeys.TradingName.toString))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        response =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body().getElementById("pageHeading").text() shouldBe s"Does the company trade under any other names than ${validCoHoCompanyDetailsResponse.companyName}?"
          result.body().getElementById("tradingName").attr("value") shouldBe ""
      }
    }

    "throw a CompanyDetailsNotFoundException if there are no company details in keystore" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, None)
      when(mockS4LService.fetchAndGet[TradingNameFormModel](Matchers.matches(CacheKeys.TradingName.toString))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      a[CompanyDetailsNotFoundException] shouldBe thrownBy (await(AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {result => status(result)} ))
    }
  }

  "calling the submitTradingName action" should {
    "redirect to the WELCOME PAGE when a user enters valid data" in new Setup {
      // TODO: Update test and description when flow is updated
      when(mockS4LService.saveForm[TradingNameFormModel](Matchers.matches(CacheKeys.TradingName.toString), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CacheMap("tstMap", Map.empty)))

      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "tradeUnderDifferentName" -> "yes",
        "tradingName" -> "Tradez R us"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/paye-registration"
      }
    }

    "return 400 when a user enters no data" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, Some(validCoHoCompanyDetailsResponse))
      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 when a user enters invalid data" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, Some(validCoHoCompanyDetailsResponse))
      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "tradeUnderDifferentName" -> "yes"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

}
