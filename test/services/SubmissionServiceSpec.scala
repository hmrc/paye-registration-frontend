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

package services

import config.AppConfig
import connectors._
import enums.IncorporationStatus
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SubmissionServiceSpec extends PayeComponentSpec with PayeFakedApp {

  implicit val request: FakeRequest[_] = FakeRequest()

  class Setup extends CodeMocks {
    val service: SubmissionService = new SubmissionService(
      payeRegistrationConnector = mockPAYERegConnector,
      keystoreConnector = mockKeystoreConnector,
      iiConnector = mockIncorpInfoConnector
    ) {
      override implicit val appConfig: AppConfig = injAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  val regId = "12345"

  def currentProfile(nRegId: String) = CurrentProfile(
    registrationID = nRegId,
    companyTaxRegistration = CompanyRegistrationProfile(
      status = "acknowledged",
      transactionId = "40-123456"
    ),
    language = "ENG",
    payeRegistrationSubmitted = false,
    None
  )

  "submitRegistration" should {
    "return a Success DES Response" in new Setup {
      when(mockPAYERegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Success))

      when(mockIncorpInfoConnector.cancelSubscription(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val cp: CurrentProfile = currentProfile(regId).copy(payeRegistrationSubmitted = true)

      when(mockKeystoreConnector.cache(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(cp))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(SessionMap("testSessionId", regId, "40-123456", Map.empty[String, JsValue])))

      await(service.submitRegistration(currentProfile(regId))) mustBe Success
    }

    "return a Cancelled DES Response" in new Setup {
      when(mockPAYERegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Cancelled))

      when(mockIncorpInfoConnector.cancelSubscription(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val cp: CurrentProfile = currentProfile(regId).copy(incorpStatus = Some(IncorporationStatus.rejected))

      when(mockKeystoreConnector.cache(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(cp))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(SessionMap("testSessionId", regId, "40-123456", Map.empty[String, JsValue])))

      await(service.submitRegistration(currentProfile(regId))) mustBe Cancelled
    }

    "return a Failed DES Response" in new Setup {
      when(mockPAYERegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Failed))

      await(service.submitRegistration(currentProfile(regId))) mustBe Failed
    }

    "return a TimedOut DES Response" in new Setup {
      when(mockPAYERegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(TimedOut))

      await(service.submitRegistration(currentProfile(regId))) mustBe TimedOut
    }
  }
}