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

import connectors.KeystoreConnect
import enums.CacheKeys
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import testHelpers.PAYERegSpec
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class SessionProfileSpec extends PAYERegSpec with DateUtil {

  object TestSession extends SessionProfile {
    override val keystoreConnector: KeystoreConnect = mockKeystoreConnector
  }

  implicit val hc = HeaderCarrier()
  def testFunc : Future[Result] = Future.successful(Ok)

  val validProfile = CurrentProfile("regId", None, CompanyRegistrationProfile("held", "txId"), "")

  "calling withCurrentProfile" should {
    "return an Ok status when a profile is found" in {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validProfile))

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe OK
    }

    "redirect to post-sign-in when no profile is found" in {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)

      val result = await(TestSession.withCurrentProfile { _ => testFunc})
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.PayeStartController.startPaye().url}")
    }
  }

}
