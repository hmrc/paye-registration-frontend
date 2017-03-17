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
import fixtures.{CoHoAPIFixture, PAYERegistrationFixture, S4LFixture}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import models.external.{CHROAddress, CompanyProfile}
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{Format, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class CompanyDetailsServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture with CoHoAPIFixture {

  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockCompRegConnector = mock[CompanyRegistrationConnector]
  val mockCohoAPIConnector = mock[CoHoAPIConnector]
  val mockCoHoService = mock[CoHoAPIService]
  val mockS4LService = mock[S4LService]

  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new CompanyDetailsSrv {
      override val cohoService: CoHoAPISrv = mockCoHoService
      override val compRegConnector: CompanyRegistrationConnect = mockCompRegConnector
      override val payeRegConnector: PAYERegistrationConnect = mockPAYERegConnector
      override val s4LService: S4LSrv = mockS4LService
      override val cohoAPIConnector: CoHoAPIConnect = mockCohoAPIConnector
    }
  }

  class NoCompanyDetailsMockedSetup {
    val service = new CompanyDetailsSrv {
      override val cohoService: CoHoAPISrv = mockCoHoService
      override val compRegConnector: CompanyRegistrationConnect = mockCompRegConnector
      override val payeRegConnector: PAYERegistrationConnect = mockPAYERegConnector
      override val s4LService: S4LSrv = mockS4LService
      override val cohoAPIConnector: CoHoAPIConnect = mockCohoAPIConnector

      override def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
        Future.successful(CompanyDetailsView(None, "test compay name", None, validROAddress, None, None))
      }

      override def saveCompanyDetails(detailsView: CompanyDetailsView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Failure)
      }
    }
  }

  class CompanyDetailsMockedSetup {
    val service = new CompanyDetailsSrv {
      override val cohoService: CoHoAPISrv = mockCoHoService
      override val compRegConnector: CompanyRegistrationConnect = mockCompRegConnector
      override val payeRegConnector: PAYERegistrationConnect = mockPAYERegConnector
      override val s4LService: S4LSrv = mockS4LService
      override val cohoAPIConnector: CoHoAPIConnect = mockCohoAPIConnector

      override def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
        Future.successful(validCompanyDetailsViewModel)
      }

      override def saveCompanyDetails(detailsView: CompanyDetailsView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Success)
      }
    }
  }

  class APIConverterMockedSetup {
    val service = new CompanyDetailsSrv {
      override val cohoService: CoHoAPISrv = mockCoHoService
      override val compRegConnector: CompanyRegistrationConnect = mockCompRegConnector
      override val payeRegConnector: PAYERegistrationConnect = mockPAYERegConnector
      override val s4LService: S4LSrv = mockS4LService
      override val cohoAPIConnector: CoHoAPIConnect = mockCohoAPIConnector

      override def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
        validCompanyDetailsViewModel
      }
    }
  }

  def companyProfile(txId: String) = CompanyProfile("held", txId)

  "Calling apiToView" should {
    "correctly produce a view model from a Company Details API model with a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        Some("trading name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.apiToView(tstModelAPI) shouldBe tstModelView
    }

    "correctly produce a view model from a Company Details API model without a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = None
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
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
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.viewToAPI(tstModelView) shouldBe Right(tstModelAPI)
    }

    "correctly produce a Company Details API model from a completed view model with 'trade under different name - no'" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.viewToAPI(tstModelView) shouldBe Right(tstModelAPI)
    }

    "return the original view model when tradingName has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@paye.co.uk"), None, None))
      )
      service.viewToAPI(tstModelView) shouldBe Left(tstModelView)
    }

    "return the original view model when business contact details has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        None
      )
      service.viewToAPI(tstModelView) shouldBe Left(tstModelView)
    }

    "return the original view model when PPOB Address has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        None,
        Some(DigitalContactDetails(Some("test@paye.co.uk"), None, None))
      )
      service.viewToAPI(tstModelView) shouldBe Left(tstModelView)
    }
  }

  "Calling getCompanyDetails" should {
    "return the correct View response when Company Details are returned from S4L" in new APIConverterMockedSetup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(Some(validCompanyDetailsViewModel)))

      await(service.getCompanyDetails("12345", "txId")) shouldBe validCompanyDetailsViewModel
    }

    "return the correct View response when Company Details are returned from the connector" in new APIConverterMockedSetup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validCompanyDetailsAPI)))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getCompanyDetails("54321", "txId")) shouldBe validCompanyDetailsViewModel
    }

    "return a default View model with company name and RO Address when no Company Details are returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockCoHoService.getStoredCompanyName()(ArgumentMatchers.any()))
        .thenReturn(Future.successful("Tst company name"))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.eq("54321"))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(companyProfile("txId")))

      when(mockCohoAPIConnector.getRegisteredOfficeAddress(ArgumentMatchers.eq("txId"))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCHROAddress))

      await(service.getCompanyDetails("54321", "txId")) shouldBe CompanyDetailsView(None, "Tst company name", None, validCHROAddress, None, None)
    }

    "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

      an[Upstream4xxResponse] shouldBe thrownBy(await(service.getCompanyDetails("54321", "txId")))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

      an[Exception] shouldBe thrownBy(await(service.getCompanyDetails("54321", "txId")))
    }

  }

  "Calling saveCompanyDetails" should {
    "return a success response when the upsert completes successfully" in new Setup {
      when(mockPAYERegConnector.upsertCompanyDetails(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsAPI))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveCompanyDetails(validCompanyDetailsViewModel, "54321")) shouldBe DownstreamOutcome.Success
    }

    "return a success response when the S4L save completes successfully" in new Setup {
      val incompleteCompanyDetailsViewModel = CompanyDetailsView(
        Some("crn"),
        "Tst Company Name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@paye.co.uk"), None, None))
      )

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.CompanyDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.saveCompanyDetails(incompleteCompanyDetailsViewModel, "54321")) shouldBe DownstreamOutcome.Success
    }
  }

  "Calling submitTradingName" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      await(service.submitTradingName(validTradingNameViewModel, "54322", "txId")) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.submitTradingName(validTradingNameViewModel, "543222", "txId")) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getPPOBPageAddresses" should {
    def viewModel(roAddr: Address, ppobAddr: Option[Address]): CompanyDetailsView = {
      CompanyDetailsView(
        crn = None,
        companyName = "unimportant",
        tradingName = None,
        roAddress = roAddr,
        ppobAddress = ppobAddr,
        businessContactDetails = None
      )
    }

    "return a single RO Address when there is no PPOOB Address" in new Setup {
      val roAddr = Address("line1", "line2", None, None, Some("postcode"), None)
      def model = viewModel(roAddr, None)
      service.getPPOBPageAddresses(model) shouldBe Map("ro" -> roAddr)
    }

    "return a single PPOB Address when the RO Address and PPOB Address are the same" in new Setup {
      val roAddr = Address("line1", "line2", None, None, Some("postcode"), None)
      def model = viewModel(roAddr, Some(roAddr))
      service.getPPOBPageAddresses(model) shouldBe Map("ppob" -> roAddr)
    }

    "return an RO Address and PPOB Address when the RO Address and PPOB Address are different" in new Setup {
      val roAddr   = Address("line1", "line2", None, None, Some("postcode"), None)
      val ppobAddr = Address("lineA", "lineB", None, None, Some("postKode"), None)
      def model = viewModel(roAddr, Some(ppobAddr))
      service.getPPOBPageAddresses(model) shouldBe Map("ro" -> roAddr, "ppob" -> ppobAddr)
    }
  }

  "Calling copyROAddrToPPOBAddr" should {
    "return a success response when copied successfully" in new CompanyDetailsMockedSetup {
      await(service.copyROAddrToPPOBAddr("54322", "txId")) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when copy is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.copyROAddrToPPOBAddr("54322", "txId")) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling submitPPOBAddr" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      await(service.submitPPOBAddr(validAddress, "54322", "txId")) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.submitPPOBAddr(validAddress, "54322", "txId")) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling submitBusinessContact" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      await(service.submitBusinessContact(validBusinessContactModel, "54322", "txId")) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.submitBusinessContact(validBusinessContactModel, "54322", "txId")) shouldBe DownstreamOutcome.Failure
    }
  }
}
