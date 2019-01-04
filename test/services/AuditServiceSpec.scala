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

package services

import helpers.PayeComponentSpec
import models.DigitalContactDetails
import models.view.PAYEContactDetails
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.audit.http.connector.AuditResult

class AuditServiceSpec extends PayeComponentSpec {
  class Setup {
    val service = new AuditService {
      override val auditConnector = mockAuditConnector
    }
  }

  val regId = "12345"

  "auditPAYEContactDetails" should {
    val tstContactDetails = PAYEContactDetails(
      name = "tstName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("tst@tst.com"),
        mobileNumber = Some("07754123456"),
        phoneNumber = Some("01214321234")
      )
    )

    "not send audit event if there was no data initially" in new Setup {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId("session-123")))

      implicit val request = FakeRequest()

      await(service.auditPAYEContactDetails(regId, tstContactDetails, None)) mustBe AuditResult.Disabled
    }
  }
}
