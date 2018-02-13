/*
 * Copyright 2018 HM Revenue & Customs
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

import enums.{CacheKeys, DownstreamOutcome}
import helpers.PayeComponentSpec
import models.api.{CompanyDetails => CompanyDetailsAPI}
import models.external.{CoHoCompanyDetailsModel, CompanyRegistrationProfile}
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Format, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future

class CompanyDetailsServiceSpec extends PayeComponentSpec with GuiceOneAppPerSuite {

  val returnHttpResponse = HttpResponse(200)

  implicit val testContext = AuthHelpers.buildAuthContext

  class Setup {
    val service = new CompanyDetailsService {
      override val incorpInfoService = mockIncorpInfoService
      override val compRegConnector  = mockCompRegConnector
      override val payeRegConnector  = mockPAYERegConnector
      override val s4LService        = mockS4LService
      override val prepopService     = mockPrepopulationService
      override val auditService      = mockAuditService
    }
  }

  class NoCompanyDetailsMockedSetup {
    val service = new CompanyDetailsService {
      override val incorpInfoService = mockIncorpInfoService
      override val compRegConnector  = mockCompRegConnector
      override val payeRegConnector  = mockPAYERegConnector
      override val s4LService        = mockS4LService
      override val prepopService     = mockPrepopulationService
      override val auditService      = mockAuditService

      override def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
        Future.successful(CompanyDetailsView("test compay name", None, Fixtures.validROAddress, None, None))
      }

      override def saveCompanyDetails(detailsView: CompanyDetailsView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Failure)
      }
    }
  }

  class CompanyDetailsMockedSetup {
    val service = new CompanyDetailsService {
      override val incorpInfoService = mockIncorpInfoService
      override val compRegConnector  = mockCompRegConnector
      override val payeRegConnector  = mockPAYERegConnector
      override val s4LService        = mockS4LService
      override val prepopService     = mockPrepopulationService
      override val auditService      = mockAuditService

      override def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
        Future.successful(Fixtures.validCompanyDetailsViewModel)
      }

      override def saveCompanyDetails(detailsView: CompanyDetailsView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Success)
      }
    }
  }

  class APIConverterMockedSetup {
    val service = new CompanyDetailsService {
      override val incorpInfoService = mockIncorpInfoService
      override val compRegConnector  = mockCompRegConnector
      override val payeRegConnector  = mockPAYERegConnector
      override val s4LService        = mockS4LService
      override val prepopService     = mockPrepopulationService
      override val auditService      = mockAuditService

      override def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
        Fixtures.validCompanyDetailsViewModel
      }
    }
  }

  def companyProfile(txId: String) = CompanyRegistrationProfile("held", txId)

  "Calling apiToView" should {
    "correctly produce a view model from a Company Details API model with a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        "Comp name",
        Some("trading name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.apiToView(tstModelAPI) mustBe tstModelView
    }

    "correctly produce a view model from a Company Details API model without a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = None
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Avenue", "Testpool", Some("TestUponAvon"), Some("Nowhereshire"), Some("LE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.apiToView(tstModelAPI) mustBe tstModelView
    }
  }

  "Calling viewToAPI" should {
    "correctly produce a Company Details API model from a completed view model" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        "Comp name",
        Some("trading name"),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.viewToAPI(tstModelView) mustBe Right(tstModelAPI)
    }

    "correctly produce a Company Details API model from a completed view model with 'trade under different name - no'" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321"))
      )
      val tstModelView = CompanyDetailsView(
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@email.com"), Some("1234567890"), Some("0987654321")))
      )
      service.viewToAPI(tstModelView) mustBe Right(tstModelAPI)
    }

    "return the original view model when tradingName has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        "Comp name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@paye.co.uk"), None, None))
      )
      service.viewToAPI(tstModelView) mustBe Left(tstModelView)
    }

    "return the original view model when business contact details has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        None
      )
      service.viewToAPI(tstModelView) mustBe Left(tstModelView)
    }

    "return the original view model when PPOB Address has not been completed" in new Setup {
      val tstModelView = CompanyDetailsView(
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = Some("trading name")
        )),
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        None,
        Some(DigitalContactDetails(Some("test@paye.co.uk"), None, None))
      )
      service.viewToAPI(tstModelView) mustBe Left(tstModelView)
    }
  }

  "Calling getCompanyDetails" should {
    "return the correct View response when Company Details are returned from S4L" in new APIConverterMockedSetup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(Some(Fixtures.validCompanyDetailsViewModel)))

      await(service.getCompanyDetails("12345", "txId")) mustBe Fixtures.validCompanyDetailsViewModel
    }

    "return the correct View response when Company Details are returned from the connector" in new APIConverterMockedSetup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(Fixtures.validCompanyDetailsAPI)))

      when(mockS4LService.saveForm(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getCompanyDetails("54321", "txId")) mustBe Fixtures.validCompanyDetailsViewModel
    }

    "return a default View model with company name and RO Address when no Company Details are returned from the connector" in new Setup {
      val tstROAddress = Address("addrLine1", "addrLine2", None, None, Some("TE9 9ST"))
      val tstCompanyDetailsModel = CoHoCompanyDetailsModel(
        companyName = "tstCompanyName",
        roAddress = tstROAddress
      )

      val tstDigitalContactDetails = DigitalContactDetails(
        email = Some("test@email.uk"),
        mobileNumber = None,
        phoneNumber = None
      )

      when(mockS4LService.fetchAndGet(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(tstCompanyDetailsModel))

      when(mockPrepopulationService.getBusinessContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(tstDigitalContactDetails)))

      when(mockS4LService.saveForm(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(companyProfile("txId")))

      await(service.getCompanyDetails("54321", "txId")) mustBe CompanyDetailsView("tstCompanyName", None, tstROAddress, None, Some(tstDigitalContactDetails))
    }

    "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

      an[Upstream4xxResponse] mustBe thrownBy(await(service.getCompanyDetails("54321", "txId")))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getCompanyDetails(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

      an[Exception] mustBe thrownBy(await(service.getCompanyDetails("54321", "txId")))
    }

  }

  "Calling saveCompanyDetails" should {
    "return a success response when the upsert completes successfully" in new Setup {
      when(mockPAYERegConnector.upsertCompanyDetails(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsAPI))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveCompanyDetails(Fixtures.validCompanyDetailsViewModel, "54321")) mustBe DownstreamOutcome.Success
    }

    "return a success response when the S4L save completes successfully" in new Setup {
      val incompleteCompanyDetailsViewModel = CompanyDetailsView(
        "Tst Company Name",
        None,
        Address("14 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK")),
        Some(Address("15 St Test Walk", "Testley", Some("Testford"), Some("Testshire"), Some("TE1 1ST"), Some("UK"))),
        Some(DigitalContactDetails(Some("test@paye.co.uk"), None, None))
      )

      when(mockS4LService.saveForm(ArgumentMatchers.contains(CacheKeys.CompanyDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CompanyDetailsView]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.saveCompanyDetails(incompleteCompanyDetailsViewModel, "54321")) mustBe DownstreamOutcome.Success
    }
  }

  "Calling submitTradingName" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      await(service.submitTradingName(Fixtures.validTradingNameViewModel, "54322", "txId")) mustBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.submitTradingName(Fixtures.validTradingNameViewModel, "543222", "txId")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling getPPOBPageAddresses" should {
    def viewModel(roAddr: Address, ppobAddr: Option[Address]): CompanyDetailsView = {
      CompanyDetailsView(
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
      service.getPPOBPageAddresses(model) mustBe Map("ro" -> roAddr)
    }

    "return a single PPOB Address when the RO Address and PPOB Address are the same" in new Setup {
      val roAddr = Address("line1", "line2", None, None, Some("postcode"), None)
      def model = viewModel(roAddr, Some(roAddr))
      service.getPPOBPageAddresses(model) mustBe Map("ppob" -> roAddr)
    }

    "return an RO Address and PPOB Address when the RO Address and PPOB Address are different" in new Setup {
      val roAddr   = Address("line1", "line2", None, None, Some("postcode"), None)
      val ppobAddr = Address("lineA", "lineB", None, None, Some("postKode"), None)
      def model = viewModel(roAddr, Some(ppobAddr))
      service.getPPOBPageAddresses(model) mustBe Map("ro" -> roAddr, "ppob" -> ppobAddr)
    }
  }

  "Calling copyROAddrToPPOBAddr" should {
    "return a success response when copied successfully" in new CompanyDetailsMockedSetup {
      await(service.copyROAddrToPPOBAddr("54322", "txId")) mustBe DownstreamOutcome.Success
    }

    "return a failure response when copy is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.copyROAddrToPPOBAddr("54322", "txId")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling submitPPOBAddr" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      await(service.submitPPOBAddr(Fixtures.validAddress, "54322", "txId")) mustBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {
      await(service.submitPPOBAddr(Fixtures.validAddress, "54322", "txId")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling submitBusinessContact" should {
    "return a success response when submit is completed successfully" in new CompanyDetailsMockedSetup {
      when(mockAuditService.auditBusinessContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(AuditResult.Success))

      implicit val request = FakeRequest()

      await(service.submitBusinessContact(Fixtures.validBusinessContactModel, "54322", "txId")) mustBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoCompanyDetailsMockedSetup {

      implicit val request = FakeRequest()
      await(service.submitBusinessContact(Fixtures.validBusinessContactModel, "54322", "txId")) mustBe DownstreamOutcome.Failure
    }
  }
}
