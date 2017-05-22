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

package utils

import common.exceptions.InternalExceptions
import connectors.{KeystoreConnect, PAYERegistrationConnector}
import enums.{CacheKeys, PAYEStatus}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import testHelpers.PAYERegSpec
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers
import play.api.test.FakeRequest

import scala.concurrent.Future


class SessionProfileSpec extends PAYERegSpec with DateUtil with InternalExceptions {

  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]

  object TestSession extends SessionProfile {
    override val keystoreConnector: KeystoreConnect = mockKeystoreConnector
    override val payeRegistrationConnector = mockPayeRegistrationConnector
  }

  implicit val hc = HeaderCarrier()
  def testFunc : Future[Result] = Future.successful(Ok)

  val validProfile = CurrentProfile("regId", None, CompanyRegistrationProfile("held", "txId"), "")

  "calling withCurrentProfile" should {
    "return an Ok status when a profile is found" in {
      implicit val request = FakeRequest()

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      when(mockPayeRegistrationConnector.getStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(PAYEStatus.draft)))

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe OK
    }

    "proceed even if the status is held and the request path is for /confirmation" in {
      implicit val confirmationRequest = FakeRequest("GET", "/register-for-paye/confirmation")

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      when(mockPayeRegistrationConnector.getStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(PAYEStatus.held)))

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe OK
    }

    "proceed even if the status is submitted and the request path is for /confirmation" in {
      implicit val confirmationRequest = FakeRequest("GET", "/register-for-paye/confirmation")

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      when(mockPayeRegistrationConnector.getStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(PAYEStatus.submitted)))

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe OK
    }

    "redirect to dashboard if the users document status is held" in {
      implicit val request = FakeRequest()

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      when(mockPayeRegistrationConnector.getStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(PAYEStatus.held)))

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.DashboardController.dashboard().url}")
    }

    "redirect to dashboard if the users document status is submitted" in {
      implicit val request = FakeRequest()

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      when(mockPayeRegistrationConnector.getStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(PAYEStatus.submitted)))

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.DashboardController.dashboard().url}")
    }

    "throw a MissingStatus exception is the users registration document doesn't contain a status" in {
      implicit val request = FakeRequest()

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      when(mockPayeRegistrationConnector.getStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingDocumentStatus](await(TestSession.withCurrentProfile { _ => testFunc}))
    }

    "redirect to post-sign-in when no profile is found" in {
      implicit val request = FakeRequest()

      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.PayeStartController.startPaye().url}")
    }
  }

}
