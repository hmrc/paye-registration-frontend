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

package controllers.userJourney

import audit.{PPOBAddressAuditEvent, PPOBAddressAuditEventDetail}
import builders.AuthBuilder
import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import fixtures.{CoHoAPIFixture, PAYERegistrationFixture, S4LFixture}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.{Address, DigitalContactDetails}
import org.jsoup._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Call, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CompanyDetailsControllerSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture with CoHoAPIFixture {
  val mockS4LService = mock[S4LService]
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockIncorpInfoService = mock[IncorporationInformationService]
  val mockAddressLookupService = mock[AddressLookupService]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]
  val mockPrepopulationService = mock[PrepopulationService]
  val mockAuditService = mock[AuditService]

  class Setup {
    val controller = new CompanyDetailsCtrl {
      override val s4LService = mockS4LService
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegistrationConnector = mockPayeRegistrationConnector
      override val authConnector = mockAuthConnector
      override val companyDetailsService = mockCompanyDetailsService
      override val incorpInfoService = mockIncorpInfoService
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val addressLookupService = mockAddressLookupService
      override val prepopService = mockPrepopulationService
      override val auditService = mockAuditService

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result], payeRegistrationSubmitted: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          "12345",
          CompanyRegistrationProfile("held", "txId"),
          "ENG",
          payeRegistrationSubmitted = false
        ))
      }
    }
  }

  val tstTradingNameModel = TradingNameView(differentName = true, tradingName = Some("test trading name"))

  val fakeRequest = FakeRequest("GET", "/")

  "calling the tradingName action" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.tradingName()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "show the correctly pre-populated trading name page when data has already been entered" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body.getElementById("pageHeading").text() shouldBe "Will the company trade under another name?"
          result.body.getElementById("differentName-true").attr("checked") shouldBe "checked"
          result.body.getElementById("differentName-false").attr("checked") shouldBe ""
          result.body.getElementById("tradingName").attr("value") shouldBe validCompanyDetailsViewModel.tradingName.get.tradingName.get
      }
    }

    "show the correctly pre-populated trading name page when negative data has already been entered" in new Setup {
      val cName = "Tst Company Name"
      val negativeTradingNameCompanyDetails = validCompanyDetailsViewModel.copy(tradingName = Some(negativeTradingNameViewModel))
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(negativeTradingNameCompanyDetails))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body.getElementById("pageHeading").text() shouldBe "Will the company trade under another name?"
          result.body.getElementById("differentName-true").attr("checked") shouldBe ""
          result.body.getElementById("differentName-false").attr("checked") shouldBe "checked"
          result.body.getElementById("tradingName").attr("value") shouldBe ""
      }
    }

    "show a blank trading name page when no Trading Name data has been entered" in new Setup {
      val cName = "Tst Company Name"
      val noTradingNameCompanyDetails = validCompanyDetailsViewModel.copy(tradingName = None)
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(noTradingNameCompanyDetails))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body().getElementById("pageHeading").text() shouldBe "Will the company trade under another name?"
          result.body.getElementById("differentName-true").parent.classNames().contains("selected") shouldBe false
          result.body.getElementById("differentName-false").parent.classNames().contains("selected") shouldBe false
          result.body().getElementById("tradingName").attr("value") shouldBe ""
      }
    }

    "show a blank trading name page when no Company Details data has been entered" in new Setup {
      val cName = "Tst Company Name"
      val defaultCompanyDetailsView = CompanyDetailsView(cName, None, validROAddress, None, None)
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(defaultCompanyDetailsView))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        response =>
          status(response) shouldBe Status.OK
          val result = Jsoup.parse(bodyOf(response))
          result.body().getElementById("pageHeading").text() shouldBe "Will the company trade under another name?"
          result.body.getElementById("differentName-true").parent.classNames().contains("selected") shouldBe false
          result.body.getElementById("differentName-false").parent.classNames().contains("selected") shouldBe false
          result.body().getElementById("tradingName").attr("value") shouldBe ""
      }
    }
  }

  "calling the submitTradingName action" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitTradingName()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }
    "redirect to the confirm ro address page when a user enters valid data" in new Setup {
      when(mockCompanyDetailsService.submitTradingName(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "differentName" -> "true",
        "tradingName" -> "Tradez R us"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/confirm-registered-office-address"
      }
    }

    "show an error page when there is an error response from the microservice" in new Setup {
      when(mockCompanyDetailsService.submitTradingName(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "differentName" -> "true",
        "tradingName" -> "Tradez R us"
      )) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return 400 when a user enters no data" in new Setup {
      when(mockCompanyDetailsService.submitTradingName(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 when a user enters invalid data" in new Setup {
      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "differentName" -> "true"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

  "roAddress" should {
    "return an ok" when {
      "the user is authorised to view the page" in new Setup {

        val testAddress =
          Address(
            "testL1",
            "testL2",
            Some("testL3"),
            Some("testL4"),
            Some("testPostCode"),
            None
          )

        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(validCompanyDetailsViewModel))

        AuthBuilder.showWithAuthorisedUser(controller.roAddress, mockAuthConnector) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.roAddress()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }
  }

  "confirm roAddress" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.confirmRO()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "redirect to next page" when {
      "the user clicks confirm" in new Setup {
        AuthBuilder.showWithAuthorisedUser(controller.confirmRO, mockAuthConnector) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.CompanyDetailsController.ppobAddress()}")
        }
      }
    }
  }

  "businessContactDetails" should {
    val bcd = DigitalContactDetails(None, None, None)

    "return an ok" when {
      "the user is authorised to view the page and there is a saved buiness contact details model" in new Setup {
        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(validCompanyDetailsViewModel.copy(businessContactDetails = Some(bcd))))

        AuthBuilder.showWithAuthorisedUser(controller.businessContactDetails, mockAuthConnector) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    "return an ok" when {
      "the user is authorised to view the page and there is no business contact details model" in new Setup {
        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(validCompanyDetailsViewModel.copy(businessContactDetails = None)))

        when(mockPrepopulationService.getBusinessContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(None))

        AuthBuilder.showWithAuthorisedUser(controller.businessContactDetails, mockAuthConnector) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    "return an ok" when {
      "the user is authorised to view the page with prepopulated data and there is no business contact details model" in new Setup {
        when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(validCompanyDetailsViewModel.copy(businessContactDetails = None)))

        when(mockPrepopulationService.getBusinessContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(validCompanyDetailsViewModel.businessContactDetails))

        when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(CacheMap("key", Map.empty)))

        AuthBuilder.showWithAuthorisedUser(controller.businessContactDetails, mockAuthConnector) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.businessContactDetails()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }
  }

  "submit businessContactDetails" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitBusinessContactDetails()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "show form errors" in new Setup {
      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      AuthBuilder.submitWithAuthorisedUser(controller.submitBusinessContactDetails(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "show error page when there is an internal error" in new Setup {
      when(mockCompanyDetailsService.submitBusinessContact(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      AuthBuilder.submitWithAuthorisedUser(controller.submitBusinessContactDetails(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "mobileNumber" -> "07123456789"
      )) {
        result =>
          status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to next page when the user submit valid data" in new Setup {
      when(mockCompanyDetailsService.submitBusinessContact(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthBuilder.submitWithAuthorisedUser(controller.submitBusinessContactDetails(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "mobileNumber" -> "07123456789"
      )) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${routes.NatureOfBusinessController.natureOfBusiness()}")
      }
    }
  }

  "ppobAddress" should {
    "return an OK" in new Setup {
      val addressMap = Map(
        "ro" -> validCompanyDetailsViewModel.roAddress,
        "ppob" -> validCompanyDetailsViewModel.ppobAddress.get
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPrepopulationService.getPrePopAddresses(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Map.empty[Int, Address]))

      when(mockCompanyDetailsService.getPPOBPageAddresses(ArgumentMatchers.any()))
        .thenReturn(addressMap)

      AuthBuilder.showWithAuthorisedUser(controller.ppobAddress, mockAuthConnector) { result =>
        status(result) shouldBe OK
      }
    }
  }

  "submitPPOBAddress" should {
    "return a BAD_REQUEST" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> ""
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      when(mockPrepopulationService.getPrePopAddresses(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Map.empty[Int, Address]))

      AuthBuilder.submitWithAuthorisedUser(controller.submitPPOBAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to business contact details if ppob is chosen" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "ppobAddress"
      )

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validCompanyDetailsViewModel))

      AuthBuilder.submitWithAuthorisedUser(controller.submitPPOBAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/register-for-paye/business-contact-details")
      }
    }

    "redirect to business contact details if ro is chosen" in new Setup {
      implicit val hc = HeaderCarrier()
      
      implicit val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "roAddress"
      )

      val auditEvent = new PPOBAddressAuditEvent(PPOBAddressAuditEventDetail(
        "testExternalUserId",
        "testAuthProviderId",
        "testRegID"
      ))

      when(mockCompanyDetailsService.copyROAddrToPPOBAddr(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockAuditService.auditPPOBAddress(ArgumentMatchers.anyString())(ArgumentMatchers.any[AuthContext](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[AnyContent]]()))
        .thenReturn(Future.successful(AuditResult.Success))

      AuthBuilder.submitWithAuthorisedUser(controller.submitPPOBAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/register-for-paye/business-contact-details")
      }
    }

    "redirect to address lookup frontend" in new Setup {
      val request = FakeRequest("GET", "/testuri?id=123456789").withFormUrlEncodedBody(
        "chosenAddress" -> "other"
      )

      when(mockAddressLookupService.buildAddressLookupUrl(ArgumentMatchers.any[String](), ArgumentMatchers.any[Call]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("testUrl"))

      AuthBuilder.submitWithAuthorisedUser(controller.submitPPOBAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "show an error page if correspondence address is chosen, somehow" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "chosenAddress" -> "correspondenceAddress"
      )

      AuthBuilder.submitWithAuthorisedUser(controller.submitPPOBAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
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

      AuthBuilder.submitWithAuthorisedUser(controller.submitPPOBAddress, mockAuthConnector, request) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "savePPOBAddress" should {

    implicit val hc = HeaderCarrier()

    "return a DownStreamOutcome SUCCESS" in new Setup {
      val expected =
        Some(Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        ))


      when(mockAddressLookupService.getAddress(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[_]]()))
        .thenReturn(Future.successful(expected))

      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopulationService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected.get))

      AuthBuilder.showWithAuthorisedUser(controller.savePPOBAddress, mockAuthConnector) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "return a DownstreamOutcome FAILURE" in new Setup {
      val expected =
        Some(Address(
          "L1",
          "L2",
          Some("L3"),
          Some("L4"),
          Some("testPostcode"),
          Some("testCode")
        ))

      when(mockAddressLookupService.getAddress(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Request[_]]()))
        .thenReturn(Future.successful(expected))

      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopulationService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(expected.get))

      AuthBuilder.showWithAuthorisedUser(controller.savePPOBAddress, mockAuthConnector) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
