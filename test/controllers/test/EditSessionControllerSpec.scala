/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys

class EditSessionControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val sessionId    = "session-id"
  val newSessionId = "new-session-id"

  class Setup {
    val controller = new EditSessionController {}
  }

  "editSession" should {

    "replace the session id with the one provided" in new Setup {
      val requestWithSession = FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
      val result = controller.setSessionId(newSessionId)(requestWithSession)

      session(result).get(SessionKeys.sessionId) mustBe Some(newSessionId)
      status(result) mustBe OK
    }
  }
}
