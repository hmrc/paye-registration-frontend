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

package controllers.test

import builders.AuthBuilder
import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import models.Address
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CompanyDetailsService, PAYEContactService, PrepopulationService}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class TestAddressLookupControllerSpec extends PAYERegSpec {

  val fakeRequest = FakeRequest("GET", "/")
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockPAYEContactService = mock[PAYEContactService]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]
  val mockPrepopService = mock[PrepopulationService]

  class Setup {
    val controller = new TestAddressLookupCtrl {
      override val authConnector = mockAuthConnector
      override val payeRegistrationConnector = mockPayeRegistrationConnector
      override val companyDetailsService = mockCompanyDetailsService
      override val payeContactService = mockPAYEContactService
      override val keystoreConnector = mockKeystoreConnector
      override val prepopService = mockPrepopService

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

  val address = Address(
    line1 = "13 Test Street",
    line2 = "No Lookup Town",
    line3 = Some("NoLookupShire"),
    line4 = None,
    postCode = None,
    country = Some("UK")
  )

  "calling the noLookup action for PPOB Address" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.noLookupPPOBAddress(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return 500 when the mocked address can't be submitted" in new Setup {
      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthBuilder.showWithAuthorisedUser(controller.noLookupPPOBAddress, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return 303 when the mocked address is successfully submitted" in new Setup {
      when(mockCompanyDetailsService.submitPPOBAddr(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(address))

      AuthBuilder.showWithAuthorisedUser(controller.noLookupPPOBAddress, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.SEE_OTHER
          redirectLocation(response) shouldBe Some(s"${controllers.userJourney.routes.CompanyDetailsController.businessContactDetails()}")
      }
    }
  }

  "calling the noLookup action for PAYE Correspondence Address" should {

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.noLookupCorrespondenceAddress(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).getOrElse("NO REDIRECT LOCATION!").contains("/gg/sign-in") shouldBe true
    }

    "return 500 when the mocked address can't be submitted" in new Setup {
      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(DownstreamOutcome.Failure))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthBuilder.showWithAuthorisedUser(controller.noLookupCorrespondenceAddress, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return 303 when the mocked address is successfully submitted" in new Setup {
      when(mockPAYEContactService.submitCorrespondence(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(DownstreamOutcome.Success))

      when(mockPrepopService.saveAddress(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      AuthBuilder.showWithAuthorisedUser(controller.noLookupCorrespondenceAddress, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.SEE_OTHER
          redirectLocation(response) shouldBe Some(s"${controllers.userJourney.routes.SummaryController.summary()}")
      }
    }
  }

}
