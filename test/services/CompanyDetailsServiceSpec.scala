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

package services

import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.{PAYERegistrationFixture, S4LFixture}
import models.view.{Address, CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import common.exceptions.DownstreamExceptions.PAYEMicroserviceException
import common.exceptions.InternalExceptions.APIConversionException
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse}

import scala.concurrent.Future

class CompanyDetailsServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture {

  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockCoHoService = mock[CoHoAPIService]
  val mockS4LService = mock[S4LService]

  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new CompanyDetailsService (mockKeystoreConnector, mockPAYERegConnector, mockCoHoService, mockS4LService)
  }

  class NoCompanyDetailsMockedSetup {
    val service = new CompanyDetailsService (mockKeystoreConnector, mockPAYERegConnector, mockCoHoService, mockS4LService) {

      override def getCompanyDetails()(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
        Future.successful(CompanyDetailsView(None, "test compay name", None, None))
      }

      override def saveCompanyDetails(detailsView: CompanyDetailsView)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Failure)
      }
    }
  }

  class CompanyDetailsMockedSetup {
    val service = new CompanyDetailsService (mockKeystoreConnector, mockPAYERegConnector, mockCoHoService, mockS4LService) {

      override def getCompanyDetails()(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
        Future.successful(validCompanyDetailsViewModel)
      }

      override def saveCompanyDetails(detailsView: CompanyDetailsView)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Success)
      }
    }
  }

  class APIConverterMockedSetup {
    val service = new CompanyDetailsService (mockKeystoreConnector, mockPAYERegConnector, mockCoHoService, mockS4LService) {

      override def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
        validCompanyDetailsViewModel
      }
    }
  }

  "Calling apiToView" should {
    "correctly produce a view model from a Company Details API model with a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        Some("trading name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        )),
        Some(Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")))
      )
      service.apiToView(tstModelAPI) shouldBe tstModelView
    }

    "correctly produce a view model from a Company Details API model without a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = None
        )),
        Some(Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")))
      )
      service.apiToView(tstModelAPI) shouldBe tstModelView
    }
  }

  "Calling viewToAPI" should {
    "correctly produce a Company Details API model from a completed view model" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        Some("trading name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        )),
        Some(Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")))
      )
      service.viewToAPI(tstModelView) shouldBe Right(tstModelAPI)
    }

    "correctly produce a Company Details API model from a completed view model with 'trade under different name - no'" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Some(Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")))
      )
      service.viewToAPI(tstModelView) shouldBe Right(tstModelAPI)
    }

    "return the original view model when tradingName has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        None,
        Some(Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")))
      )
      service.viewToAPI(tstModelView) shouldBe Left(tstModelView)
    }

    "return the original view model when address has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        None
      )
      service.viewToAPI(tstModelView) shouldBe Left(tstModelView)
    }
  }

  "Calling getCompanyDetails" should {
    "return the correct View response when Company Details are returned from the S4L" in new APIConverterMockedSetup {
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.CompanyDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(Some(validCompanyDetailsViewModel)))

      await(service.getCompanyDetails()) shouldBe validCompanyDetailsViewModel
    }

    "return the correct View response when Company Details are returned from the connector" in new APIConverterMockedSetup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.CompanyDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(validCompanyDetailsAPI)))

      when(mockS4LService.saveForm(Matchers.eq(CacheKeys.CompanyDetails.toString),Matchers.any)(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getCompanyDetails()) shouldBe validCompanyDetailsViewModel
    }

    "return a default View model when no Company Details are returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.CompanyDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      when(mockCoHoService.getStoredCompanyName()(Matchers.any()))
        .thenReturn(Future.successful("Tst company name"))

      when(mockS4LService.saveForm(Matchers.eq(CacheKeys.CompanyDetails.toString),Matchers.any)(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getCompanyDetails()) shouldBe CompanyDetailsView(None, "Tst company name", None, None)
    }

    "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.CompanyDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

      an[Upstream4xxResponse] shouldBe thrownBy(await(service.getCompanyDetails()))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

      an[Exception] shouldBe thrownBy(await(service.getCompanyDetails()))
    }

  }

  "Calling saveCompanyDetails" should {
    "return a success response when the upsert completes successfully" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.upsertCompanyDetails(Matchers.contains("54321"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsAPI))

      when(mockS4LService.clear()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveCompanyDetails(validCompanyDetailsViewModel)) shouldBe DownstreamOutcome.Success
    }

    "return a success response when the S4L save completes successfully" in new Setup {
      val incompleteCompanyDetailsViewModel = CompanyDetailsView(
        Some("crn"),
        "Tst Company Name",
        None,
        Some(Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")))
      )

      when(mockS4LService.saveForm(Matchers.eq(CacheKeys.CompanyDetails.toString),Matchers.any)(Matchers.any[HeaderCarrier](), Matchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.saveCompanyDetails(incompleteCompanyDetailsViewModel)) shouldBe DownstreamOutcome.Success
    }
  }

  "Calling submitTradingName" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      mockFetchRegID("54322")

      await(service.submitTradingName(validTradingNameViewModel)) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {
      mockFetchRegID("54322")

      await(service.submitTradingName(validTradingNameViewModel)) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getROAddress" should {
    "return an address successfully" in new CompanyDetailsMockedSetup {
      mockFetchRegID("54321")
      await(service.getROAddress()) shouldBe validCompanyDetailsViewModel.roAddress.get
    }
  }

  "Calling submitROAddress" should {
    "return a success response when the address is saved successfully" in new CompanyDetailsMockedSetup {
      mockFetchRegID("54321")
      await(service.submitROAddress(validCompanyDetailsViewModel.roAddress.get)) shouldBe DownstreamOutcome.Success
    }
  }
}
