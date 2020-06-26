/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AppConfig
import enums.{IncorporationStatus, PAYEStatus}
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class CurrentProfileServiceSpec extends PayeComponentSpec with PayeFakedApp {

  class Setup {
    val service = new CurrentProfileService {
      override val businessRegistrationConnector = mockBusinessRegistrationConnector
      override val companyRegistrationConnector = mockCompRegConnector
      override val payeRegistrationConnector = mockPAYERegConnector
      override val keystoreConnector = mockKeystoreConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override implicit val appConfig: AppConfig = mockAppConfig
    }
  }

  val validBusinessProfile = Fixtures.validBusinessRegistrationResponse
  val validCompanyProfile = CompanyRegistrationProfile("held", "txId")

  val validCurrentProfile = CurrentProfile(
    validBusinessProfile.registrationID,
    validCompanyProfile,
    validBusinessProfile.language,
    payeRegistrationSubmitted = false,
    incorpStatus = Some(IncorporationStatus.accepted)
  )

  "fetchAndStoreCurrentProfile" should {

    "Return a successful outcome after successfully storing a valid Business Registration response" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(validBusinessProfile))

      when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      when(mockKeystoreConnector.cache(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(SessionMap("", "", "", Map.empty[String, JsValue])))

      when(mockIncorpInfoConnector.setupSubscription(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(IncorporationStatus.accepted)))

      when(mockPAYERegConnector.getStatus(ArgumentMatchers.contains(validBusinessProfile.registrationID))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(PAYEStatus.draft)))

      await(service.fetchAndStoreCurrentProfile) mustBe validCurrentProfile
    }

    "Return a successful outcome after nothing is returned from II subscription" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(validBusinessProfile))

      when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      when(mockKeystoreConnector.cache(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(SessionMap("", "", "", Map.empty[String, JsValue])))

      when(mockIncorpInfoConnector.setupSubscription(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getStatus(ArgumentMatchers.contains(validBusinessProfile.registrationID))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(PAYEStatus.draft)))

      await(service.fetchAndStoreCurrentProfile) mustBe validCurrentProfile.copy(incorpStatus = None)
    }

    "Return an unsuccessful outcome when there is no record in Business Registration" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when the user is not authorised for Business Registration" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("")))

      intercept[ForbiddenException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when Business Registration returns an error response" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("")))

      intercept[RuntimeException](await(service.fetchAndStoreCurrentProfile))
    }
  }

  "regSubmitted" should {
    "return false when status is draft" in new Setup {
      service.regSubmitted(Some(PAYEStatus.draft)) mustBe false
    }

    "return false when status is invalid" in new Setup {
      service.regSubmitted(Some(PAYEStatus.invalid)) mustBe false
    }

    "return true when status is neither draft nor invalid" in new Setup {
      service.regSubmitted(Some(PAYEStatus.held)) mustBe true
      service.regSubmitted(Some(PAYEStatus.submitted)) mustBe true
      service.regSubmitted(Some(PAYEStatus.acknowledged)) mustBe true
      service.regSubmitted(Some(PAYEStatus.rejected)) mustBe true
      service.regSubmitted(Some(PAYEStatus.cancelled)) mustBe true
    }

    "return false when no status is returned" in new Setup {
      service.regSubmitted(None) mustBe false
    }
  }

  "updateCurrentProfileWithIncorpStatus" should {
    "return some regId" in new Setup {

      val testSessionMap = SessionMap(
        sessionId = "testSessionId",
        registrationId = validCurrentProfile.registrationID,
        transactionId = validCurrentProfile.companyTaxRegistration.transactionId,
        data = Map(
          "CurrentProfile" -> Json.toJson(validCurrentProfile)
        )
      )

      when(mockKeystoreConnector.fetchByTransactionId(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(Some(testSessionMap)))

      when(mockKeystoreConnector.cacheSessionMap(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future(testSessionMap))

      val result = await(service.updateCurrentProfileWithIncorpStatus(txId = "testTxId", status = IncorporationStatus.rejected))
      result mustBe Some(validCurrentProfile.registrationID)
    }
  }
}
