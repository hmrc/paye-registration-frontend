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

import builders.AuthBuilder
import connectors.{Failed, PAYERegistrationConnector, Success, TimedOut}
import enums.{CacheKeys, UserCapacity}
import fixtures.{CoHoAPIFixture, KeystoreFixture}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SubmissionServiceSpec extends PAYERegSpec with KeystoreFixture with CoHoAPIFixture with AuthBuilder {

  val mockPayeRegConnector = mock[PAYERegistrationConnector]

  trait Setup {
    val service = new SubmissionSrv {
      override val payeRegistrationConnector = mockPayeRegConnector
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  val regId = "12345"
  implicit val hc = HeaderCarrier()

  def currentProfile(regId: String) = CurrentProfile(
    registrationID = regId,
    companyTaxRegistration = CompanyRegistrationProfile(
      status = "acknowledged",
      transactionId = "40-123456"
    ),
    language = "ENG",
    payeRegistrationSubmitted = false
  )

  "submitRegistration" should {
    "return a Success DES Response" in new Setup {
      when(mockPayeRegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Success))

      mockKeystoreCache(CacheKeys.CurrentProfile.toString, CacheMap("CurrentProfile", Map.empty))

      await(service.submitRegistration(currentProfile(regId))) shouldBe Success
    }
    "return a Failed DES Response" in new Setup {
      when(mockPayeRegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Failed))

      await(service.submitRegistration(currentProfile(regId))) shouldBe Failed
    }
    "return a TimedOut DES Response" in new Setup {
      when(mockPayeRegConnector.submitRegistration(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(TimedOut))

      await(service.submitRegistration(currentProfile(regId))) shouldBe TimedOut
    }
  }

}
