/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.test

import enums.DownstreamOutcome
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.Address
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future

class TestAddressLookupControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  class Setup {
    val controller: TestAddressLookupController = new TestAddressLookupController(
      companyDetailsService = mockCompanyDetailsService,
      keystoreConnector = mockKeystoreConnector,
      payeContactService = mockPAYEContactService,
      authConnector = mockAuthConnector,
      s4LService = mockS4LService,
      incorpInfoService = mockIncorpInfoService,
      prepopService = mockPrepopService,
      incorporationInformationConnector = mockIncorpInfoConnector,
      payeRegistrationService = mockPayeRegService,
      mcc = mockMcc
    )(injAppConfig, ec)
     {
      override lazy val redirectToLogin: Result = MockAuthRedirects.redirectToLogin
      override lazy val redirectToPostSign: Result = MockAuthRedirects.redirectToPostSign
    }
  }

  val address: Address = Address(
    line1 = "13 Test Street",
    line2 = "No Lookup Town",
    line3 = Some("NoLookupShire"),
    line4 = None,
    postCode = None,
    country = Some("UK")
  )

  "calling the noLookup action for PPOB Address" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.noLookupPPOBAddress, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return 500 when the mocked address can't be submitted" in new Setup {
      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthHelpers.showAuthorisedWithCP(controller.noLookupPPOBAddress, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 303 when the mocked address is successfully submitted" in new Setup {
      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthHelpers.showAuthorisedWithCP(controller.noLookupPPOBAddress, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe SEE_OTHER
          redirectLocation(response) mustBe Some(s"${controllers.userJourney.routes.CompanyDetailsController.businessContactDetails}")
      }
    }
  }

  "calling the noLookup action for PAYE Correspondence Address" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.noLookupCorrespondenceAddress, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return 500 when the mocked address can't be submitted" in new Setup {
      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthHelpers.showAuthorisedWithCP(controller.noLookupCorrespondenceAddress, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 303 when the mocked address is successfully submitted" in new Setup {
      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthHelpers.showAuthorisedWithCP(controller.noLookupCorrespondenceAddress, Fixtures.validCurrentProfile, fakeRequest) {
        response =>
          status(response) mustBe SEE_OTHER
          redirectLocation(response) mustBe Some(s"${controllers.userJourney.routes.SummaryController.summary}")
      }
    }
  }
}