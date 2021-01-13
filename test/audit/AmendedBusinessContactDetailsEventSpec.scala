/*
 * Copyright 2021 HM Revenue & Customs
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
import models.DigitalContactDetails
import play.api.libs.json.Json

class AmendedBusinessContactDetailsEventSpec extends PayeComponentSpec {
  val externalUserId = "testExternalUserId"
  val authProviderId = "testAuthProviderId"

  "AmendedBusinessContactDetailsEventDetail" should {
    "construct a full set of Json" in {
      val testModel = AmendedBusinessContactDetailsEventDetail(
        externalUserId,
        authProviderId,
        "testRegId",
        DigitalContactDetails(
          Some("test@email.com"),
          Some("1234567890"),
          Some("1234567892")),
        DigitalContactDetails(
          Some("sample@googlemail.com"),
          Some("7890564320"),
          Some("1234567892"))
      )

      val expectedJson = Json.parse(
        """
          |{
          | "externalUserId" : "testExternalUserId",
          | "authProviderId" : "testAuthProviderId",
          | "journeyId" : "testRegId",
          | "previousContactDetails" : {
          |   "email" : "test@email.com",
          |   "mobileNumber" : "1234567890",
          |   "phoneNumber" :  "1234567892"
          | },
          | "newContactDetails" : {
          |  "email" : "sample@googlemail.com",
          |   "mobileNumber" : "7890564320",
          |   "phoneNumber" :  "1234567892"
          |}
          |}
        """.stripMargin
      )

      val result = Json.toJson[AmendedBusinessContactDetailsEventDetail](testModel)
      result mustBe expectedJson
    }
  }
}
