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

import enums.{CacheKeys, IncorporationStatus, RegistrationDeletion}
import helpers.PayeComponentSpec
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import org.mockito.Mockito._
import services.PAYERegistrationService

import scala.concurrent.Future


class SessionProfileSpec extends PayeComponentSpec {

  class Setup extends CodeMocks {
    val testSession = new SessionProfile {
      override val keystoreConnector                  = mockKeystoreConnector
      override val incorporationInformationConnector  = mockIncorpInfoConnector
      override val payeRegistrationService            = mockPayeRegService
    }
  }

  def testFunc : Future[Result] = Future.successful(Ok)
  implicit val request = FakeRequest()

  def validProfile(regSubmitted: Boolean, ackRefStatus : Option[String] = None)
    = CurrentProfile("regId", CompanyRegistrationProfile("held", "txId", ackRefStatus), "", regSubmitted, None)

  "calling withCurrentProfile" should {
    "carry out the passed function when it is in the Session Repository" when {
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
    "carry out the passed function when nothing is in the Session Repository but there is Something in keystore" when {
      "payeRegistrationSubmitted is 'false' and ct is pending" in new Setup {
        val cp = Some(validProfile(false, Some("04")))
        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)
        when(mockKeystoreConnector.fetchAndGetFromKeystore(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(cp))

        when(mockIncorpInfoConnector.setupSubscription(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe OK
      }

      "payeRegistrationSubmitted is 'false' and ct is accepted" in new Setup {
        val cp = Some(validProfile(false, Some("04")))
        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)
        when(mockKeystoreConnector.fetchAndGetFromKeystore(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(cp))

        when(mockIncorpInfoConnector.setupSubscription(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(IncorporationStatus.accepted)))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe OK
      }

      "payeRegistrationSubmitted is 'false' and ct is rejected" in new Setup {
        val cp = Some(validProfile(false, Some("04")))
        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)
        when(mockKeystoreConnector.fetchAndGetFromKeystore(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(cp))

        when(mockIncorpInfoConnector.setupSubscription(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(IncorporationStatus.rejected)))

        when(mockPayeRegService.handleIIResponse(ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.success))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe SEE_OTHER
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
      "currentProfile is not in SessionRepository & Keystore" in new Setup {
        when(mockKeystoreConnector.fetchAndGetFromKeystore(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.PayeStartController.startPaye()}")
      }
    }

    "redirect to CT incorporation rejected page" when {
      "incorporation is rejected in currentProfile" in new Setup {
        val cp = validProfile(false).copy(incorpStatus = Some(IncorporationStatus.rejected))
        mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(cp))

        val result = testSession.withCurrentProfile { _ => testFunc }
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.SignInOutController.incorporationRejected()}")
      }
    }
  }
  "currentProfileChecks" should {
    s"redirect user to ${controllers.userJourney.routes.SignInOutController.postSignIn().url}" in new Setup {
      val cp = validProfile(regSubmitted = false).copy(companyTaxRegistration = CompanyRegistrationProfile("foo","bar",Some("6")))

      val res = testSession.currentProfileChecks(cp)(_ => Future.successful(Ok))
      redirectLocation(res) mustBe Some(s"${controllers.userJourney.routes.SignInOutController.postSignIn()}")
    }
    s"redirect user to otrs" in new Setup {
      val cp = validProfile(regSubmitted = false).copy(companyTaxRegistration = CompanyRegistrationProfile("draft","bar",Some("3")))

      val res = testSession.currentProfileChecks(cp)(_ => Future.successful(Ok))
      redirectLocation(res) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
    }
    s"redirect user to ${controllers.userJourney.routes.DashboardController.dashboard().url}" in new Setup {
      val cp = validProfile(regSubmitted = true)

      val res = testSession.currentProfileChecks(cp)(_ => Future.successful(Ok))
      redirectLocation(res) mustBe Some(s"${controllers.userJourney.routes.DashboardController.dashboard()}")
    }
    s"redirect user to ${controllers.userJourney.routes.SignInOutController.incorporationRejected().url}" in new Setup{
      val cp = validProfile(regSubmitted = false).copy(incorpStatus = Some(IncorporationStatus.rejected))

      val res = testSession.currentProfileChecks(cp)(_ => Future.successful(Ok))
      redirectLocation(res) mustBe Some(s"${controllers.userJourney.routes.SignInOutController.incorporationRejected()}")
    }
    s"carry on with passed in function and return 200" in new Setup {
      val res = testSession.currentProfileChecks(validProfile(false))(_ => Future.successful(Ok))
      status(res) mustBe 200
    }
  }
}
