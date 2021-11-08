/*
 * Copyright 2021 HM Revenue & Customs
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

import enums.DownstreamOutcome
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.AuditingInformation
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.{Address, DigitalContactDetails}
import org.jsoup._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.pages.companyDetails.{businessContactDetails, confirmROAddress, ppobAddress, tradingName}
import views.html.pages.error.restart

import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}
import scala.concurrent.{ExecutionContext, Future}

class CompanyDetailsControllerSpec extends PayeComponentSpec with PayeFakedApp {

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockTradingNamePage: tradingName = app.injector.instanceOf[tradingName]
  lazy val mockRestartPage: restart = app.injector.instanceOf[restart]
  lazy val mockBusinessContactDetailsPage: businessContactDetails = app.injector.instanceOf[businessContactDetails]
  lazy val mockConfirmROAddress: confirmROAddress = app.injector.instanceOf[confirmROAddress]
  lazy val mockPPOBAddressPage: ppobAddress = app.injector.instanceOf[ppobAddress]

  class Setup {
    val controller = new CompanyDetailsController(
      mockS4LService,
      mockKeystoreConnector,
      mockCompanyDetailsService,
      mockIncorpInfoService,
      mockAuthConnector,
      mockAddressLookupService,
      mockPrepopulationService,
      mockAuditService,
      mockIncorpInfoConnector,
      mockPayeRegService,
      mockMcc,
      mockTradingNamePage,
      mockRestartPage,
      mockBusinessContactDetailsPage,
      mockConfirmROAddress,
      mockPPOBAddressPage
    )(mockAppConfig,
      globalExecutionContext
    )
  }

  val companyNameKey: String = "CompanyName"

  val tstTradingNameModel = TradingNameView(differentName = true, tradingName = Some("test trading name"))

  val fakeRequest = FakeRequest("GET", "/")

  "calling the tradingName action" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.tradingName, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "show the correctly pre-populated trading name page when data has already been entered" in new Setup {
      when(mockCompanyDetailsService.withLatestCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))
      when(mockCompanyDetailsService.getTradingNamePrepop(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showAuthorisedWithCP(controller.tradingName, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
          val result = Jsoup.parse(contentAsString(response))
          result.body.getElementById("pageHeading").text() mustBe "Does or will the company trade using a different name?"
          result.body.getElementById("differentName-true").attr("checked") mustBe "checked"
          result.body.getElementById("differentName-false").attr("checked") mustBe ""
          result.body.getElementById("tradingName").attr("value") mustBe Fixtures.validCompanyDetailsViewModel.tradingName.get.tradingName.get
      }
    }

    "show the correctly pre-populated trading name page when negative data has already been entered" in new Setup {
      val negativeTradingNameCompanyDetails = Fixtures.validCompanyDetailsViewModel.copy(tradingName = Some(Fixtures.negativeTradingNameViewModel))
      when(mockCompanyDetailsService.withLatestCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(negativeTradingNameCompanyDetails))
      when(mockCompanyDetailsService.getTradingNamePrepop(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("foo bar")))

      AuthHelpers.showAuthorisedWithCP(controller.tradingName, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
          val result = Jsoup.parse(contentAsString(response))
          result.body.getElementById("pageHeading").text() mustBe "Does or will the company trade using a different name?"
          result.body.getElementById("differentName-true").attr("checked") mustBe ""
          result.body.getElementById("differentName-false").attr("checked") mustBe "checked"
          result.body.getElementById("tradingName").attr("value") mustBe "foo bar"
      }
    }

    "show a blank trading name page when no Trading Name data has been entered and no pre pop exists" in new Setup {
      val noTradingNameCompanyDetails = Fixtures.validCompanyDetailsViewModel.copy(tradingName = None)
      when(mockCompanyDetailsService.withLatestCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(noTradingNameCompanyDetails))
      when(mockCompanyDetailsService.getTradingNamePrepop(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showAuthorisedWithCP(controller.tradingName, Fixtures.validCurrentProfile, fakeRequest) {
        (response: Future[Result]) =>
          status(response) mustBe Status.OK
          val result = Jsoup.parse(contentAsString(response))
          result.body().getElementById("pageHeading").text() mustBe "Does or will the company trade using a different name?"
          result.body.getElementById("differentName-true").attr("checked") mustBe ""
          result.body.getElementById("differentName-false").attr("checked") mustBe ""
          result.body().getElementById("tradingName").attr("value") mustBe ""
      }
    }

    "show a pre popped trading name page when no Company Details data has been entered but pre pop returns a string" in new Setup {
      val cName = "Tst Company Name"
      val defaultCompanyDetailsView = CompanyDetailsView(cName, None, Fixtures.validROAddress, None, None)
      when(mockCompanyDetailsService.withLatestCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(defaultCompanyDetailsView))
      when(mockCompanyDetailsService.getTradingNamePrepop(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("foo bar wizz")))
      AuthHelpers.showAuthorisedWithCP(controller.tradingName, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe Status.OK
          val result = Jsoup.parse(contentAsString(response))
          result.body().getElementById("pageHeading").text() mustBe "Does or will the company trade using a different name?"
          result.body.getElementById("differentName-true").attr("checked") mustBe ""
          result.body.getElementById("differentName-false").attr("checked") mustBe ""
          result.body().getElementById("tradingName").attr("value") mustBe "foo bar wizz"
      }
    }
  }

  "calling the submitTradingName action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.submitTradingName, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "redirect to the confirm ro address page when a user enters valid data" in new Setup {
      when(mockCompanyDetailsService.submitTradingName(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(controller.submitTradingName, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "differentName" -> "true",
        "tradingName" -> "Tradez R us"
      )) {
        result =>
          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some("/register-for-paye/confirm-registered-office-address")
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      when(mockCompanyDetailsService.submitTradingName(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthHelpers.submitAuthorisedWithCP(controller.submitTradingName, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "differentName" -> "true",
        "tradingName" -> "Tradez R us"
      )) {
        result =>
          status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return 400 when a user enters no data" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockCompanyDetailsService.submitTradingName(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCoHoCompanyDetailsResponse))

      AuthHelpers.submitAuthorisedWithCP(controller.submitTradingName, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) mustBe Status.BAD_REQUEST
      }
    }

    "return 400 when a user enters invalid data" in new Setup {
      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCoHoCompanyDetailsResponse))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      AuthHelpers.submitAuthorisedWithCP(controller.submitTradingName, Fixtures.validCurrentProfile, fakeRequest.withFormUrlEncodedBody(
        "differentName" -> "true"
      )) {
        result =>
          status(result) mustBe Status.BAD_REQUEST
      }
    }
  }

  "roAddress" should {
    "return an ok" when {
      "the user is authorised to view the page" in new Setup {
        when(mockCompanyDetailsService.withLatestCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

        AuthHelpers.showAuthorisedWithCP(controller.roAddress, Fixtures.validCurrentProfile, fakeRequest) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.roAddress, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }
  }

  "confirm roAddress" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.confirmRO, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "redirect to next page" when {
      "the user clicks confirm" in new Setup {
        AuthHelpers.showAuthorisedWithCP(controller.confirmRO, Fixtures.validCurrentProfile, fakeRequest) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.CompanyDetailsController.ppobAddress()}")
        }
      }
    }
  }

  "businessContactDetails" should {
    val bcd = DigitalContactDetails(None, None, None)

    "return an ok" when {
      "the user is authorised to view the page and there is a saved buiness contact details model" in new Setup {
        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel.copy(businessContactDetails = Some(bcd))))

        AuthHelpers.showAuthorisedWithCP(controller.businessContactDetails, Fixtures.validCurrentProfile, fakeRequest) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    "return an ok" when {
      "the user is authorised to view the page and there is no business contact details model" in new Setup {
        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel.copy(businessContactDetails = None)))

        when(mockPrepopulationService.getBusinessContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(None))

        AuthHelpers.showAuthorisedWithCP(controller.businessContactDetails, Fixtures.validCurrentProfile, fakeRequest) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    "return an ok" when {
      "the user is authorised to view the page with prepopulated data and there is no business contact details model" in new Setup {
        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel.copy(businessContactDetails = None)))

        when(mockPrepopulationService.getBusinessContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel.businessContactDetails))

        when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(CacheMap("key", Map.empty)))

        AuthHelpers.showAuthorisedWithCP(controller.businessContactDetails, Fixtures.validCurrentProfile, fakeRequest) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.businessContactDetails, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }
  }

  "submit businessContactDetails" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.submitBusinessContactDetails, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "show form errors" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitBusinessContactDetails, Fixtures.validCurrentProfile, fakeRequest.withSession(companyNameKey -> "FakeCompany").withFormUrlEncodedBody()) {
        result =>
          status(result) mustBe BAD_REQUEST
      }
    }

    "show error page when there is an internal error" in new Setup {
      when(mockCompanyDetailsService.submitBusinessContact(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitBusinessContactDetails, Fixtures.validCurrentProfile, fakeRequest.withSession(companyNameKey -> "FakeCompany").withFormUrlEncodedBody(
        "mobileNumber" -> "07123456789"
      )) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to next page when the user submit valid data" in new Setup {
      when(mockCompanyDetailsService.submitBusinessContact(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitBusinessContactDetails, Fixtures.validCurrentProfile, fakeRequest.withSession(companyNameKey -> "FakeCompany").withFormUrlEncodedBody(
        "mobileNumber" -> "07123456789"
      )) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(s"${routes.NatureOfBusinessController.natureOfBusiness()}")
      }
    }
  }

  "ppobAddress" should {
    "return an OK" in new Setup {
      val addressMap = Map(
        "ro" -> Fixtures.validCompanyDetailsViewModel.roAddress,
        "ppob" -> Fixtures.validCompanyDetailsViewModel.ppobAddress.get
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPrepopulationService.getPrePopAddresses(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Map.empty[Int, Address]))

      when(mockCompanyDetailsService.getPPOBPageAddresses(ArgumentMatchers.any()))
        .thenReturn(addressMap)

      AuthHelpers.showAuthorisedWithCP(controller.ppobAddress, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe OK
      }
    }
  }

  "submitPPOBAddress" should {
    "return a BAD_REQUEST" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> ""
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      when(mockPrepopulationService.getPrePopAddresses(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Map(1 -> Fixtures.validCompanyDetailsViewModel.roAddress)))

      when(mockCompanyDetailsService.getPPOBPageAddresses(ArgumentMatchers.any()))
        .thenReturn(Map(
          "ro" -> Fixtures.validCompanyDetailsViewModel.roAddress,
          "ppob" -> Fixtures.validCompanyDetailsViewModel.ppobAddress.get
        ))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitPPOBAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to business contact details if ppob is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "ppobAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsViewModel))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitPPOBAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/register-for-paye/business-contact-details")
      }
    }

    "redirect to business contact details if ro is chosen" in new Setup {
      implicit val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "roAddress"
      )

      when(mockCompanyDetailsService.copyROAddrToPPOBAddr(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockAuditService.auditPPOBAddress(ArgumentMatchers.anyString())(ArgumentMatchers.any[AuditingInformation](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[AnyContent]], ArgumentMatchers.any[ExecutionContext]()))
        .thenReturn(Future.successful(AuditResult.Success))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitPPOBAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/register-for-paye/business-contact-details")
      }
    }

    "redirect to address lookup frontend" in new Setup {
      val request = FakeRequest("GET", "/testuri?id=123456789").withFormUrlEncodedBody(
        "chosenAddress" -> "other"
      )

      when(mockAddressLookupService.buildAddressLookupUrl(ArgumentMatchers.any[String](), ArgumentMatchers.any[Call]())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Messages]))
        .thenReturn(Future.successful("testUrl"))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitPPOBAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
      }
    }

    "show an error page if correspondence address is chosen, somehow" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "correspondenceAddress"
      )

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitPPOBAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to error page when fail saving PPOB Address" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "prepopAddress13"
      )

      val address = Address(
        "testL1",
        "testL2",
        Some("testL3"),
        Some("testL4"),
        Some("testPostCode"),
        None
      )

      when(mockPrepopulationService.getAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(address))

      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthHelpers.submitAuthorisedWithCPAndAudit(controller.submitPPOBAddress, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "savePPOBAddress" should {
    "return a DownStreamOutcome SUCCESS" in new Setup {
      val testAlfId = "1234567890"
      val expected =
        Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        )


      when(mockAddressLookupService.getAddress(ArgumentMatchers.contains(testAlfId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected))

      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopulationService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected))

      AuthHelpers.showAuthorisedWithCP(controller.savePPOBAddress(Some(testAlfId)), Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe SEE_OTHER
      }
    }

    "return a DownstreamOutcome FAILURE" in new Setup {
      val testAlfId = "1234567890"
      val expected =
        Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        )

      when(mockAddressLookupService.getAddress(ArgumentMatchers.contains(testAlfId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected))

      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopulationService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected))

      AuthHelpers.showAuthorisedWithCP(controller.savePPOBAddress(Some(testAlfId)), Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
