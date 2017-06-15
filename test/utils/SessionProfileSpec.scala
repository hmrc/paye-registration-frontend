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
  implicit val request = FakeRequest()

  def validProfile(regSubmitted: Boolean) = CurrentProfile("regId", None, CompanyRegistrationProfile("held", "txId"), "", regSubmitted)

  "calling withCurrentProfile" should {
    "carry out the passed function" when {
      "payeRegistrationSubmitted is 'false'" in {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(false)))

        val result = await(TestSession.withCurrentProfile { _ => testFunc })
        status(result) shouldBe OK
      }

      "payeRegistrationSubmitted is 'true' and withCurrentProfile is called specifying no check of submission status" in {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(true)))

        val result = await(TestSession.withCurrentProfile ({ _ => testFunc }, checkSubmissionStatus = false))
        status(result) shouldBe OK
      }
    }

    "redirect to dashboard" when {
      "payeRegistrationSubmitted is 'true'" in {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(true)))

        val result = await(TestSession.withCurrentProfile { _ => testFunc })
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.DashboardController.dashboard()}")
      }
    }

    "redirect to start of journey" when {
      "currentProfile is not in Keystore" in {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)

        val result = await(TestSession.withCurrentProfile { _ => testFunc })
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.PayeStartController.startPaye()}")
      }
    }
  }
}
