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

package utils

import enums.CacheKeys
import helpers.PayeComponentSpec
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest

import scala.concurrent.Future


class SessionProfileSpec extends PayeComponentSpec {

  class Setup extends CodeMocks {
    val testSession = new SessionProfile {
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  def testFunc : Future[Result] = Future.successful(Ok)
  implicit val request = FakeRequest()

  def validProfile(regSubmitted: Boolean, ackRefStatus : Option[String] = None)
    = CurrentProfile("regId", CompanyRegistrationProfile("held", "txId", ackRefStatus), "", regSubmitted, None)

  "calling withCurrentProfile" should {
    "carry out the passed function" when {
      "payeRegistrationSubmitted is 'false'" in new Setup {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(false)))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe OK
      }

      "payeRegistrationSubmitted is 'false' and ct is accepted" in new Setup {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(false, Some("04"))))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe OK
      }

      "payeRegistrationSubmitted is 'true' and withCurrentProfile is called specifying no check of submission status" in new Setup {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(true)))

        val result = testSession.withCurrentProfile ({ _ => testFunc }, checkSubmissionStatus = false)
        status(result) mustBe OK
      }
    }

    "redirect to post sign in" when {
      "ct is rejected in current profile" in new Setup {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(true, Some("06"))))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.SignInOutController.postSignIn()}")
      }
    }

    "redirect to dashboard" when {
      "payeRegistrationSubmitted is 'true'" in new Setup {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile(true)))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.DashboardController.dashboard()}")
      }
    }

    "redirect to start of journey" when {
      "currentProfile is not in Keystore" in new Setup {

        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.PayeStartController.startPaye()}")
      }
    }
  }
}
