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

package audit

import helpers.PayeComponentSpec
import play.api.libs.json.Json

class PPOBAddressAuditEventSpec extends PayeComponentSpec {
  val externalUserId = "testExternalUserId"
  val authProviderId = "testAuthProviderId"

  "PPOBAddressAuditEventDetail" should {
    "construct a full set of Json" in {
      val testModel = PPOBAddressAuditEventDetail(externalUserId, authProviderId, "testRegId")

      val expectedJson = Json.parse(
        """
          |{
          | "externalUserId" : "testExternalUserId",
          | "authProviderId" : "testAuthProviderId",
          | "journeyId" : "testRegId",
          | "registeredOfficeAddress" : "true"
          |}
        """.stripMargin
      )

      val result = Json.toJson[PPOBAddressAuditEventDetail](testModel)(PPOBAddressAuditEventDetail.writes)
      result mustBe expectedJson
    }
  }
}
