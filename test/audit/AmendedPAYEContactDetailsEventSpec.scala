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

package audit

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class AmendedPAYEContactDetailsEventSpec extends UnitSpec {
  "AmendedPAYEContactDetailsEvent" should {
    "construct full Json as per definition" in {
      val testAmendedPAYEContactDetailsEvent = AmendedPAYEContactDetailsEventDetail(
        externalUserId = "ext-12354",
        authProviderId = "ap-12345",
        journeyId      = "12345",
        previousPAYEContactDetails = AuditPAYEContactDetails(
          contactName   = "TestContact Name",
          email         = Some("test@email.com"),
          mobileNumber  = Some("0987654321"),
          phoneNumber   = Some("0987654321")
        ),
        newPAYEContactDetails = AuditPAYEContactDetails(
          contactName   = "TestContact Name",
          email         = Some("test@email.co.uk"),
          mobileNumber  = Some("1234567890"),
          phoneNumber   = Some("1234567890")
        )
      )

      val testExpectedJson = Json.parse(
        """
          |{
          | "externalUserId" : "ext-12345",
          | "authProviderId" : "ap-12345",
          | "journeyId" : "12345",
          | "previousPAYEContactDetails" : {
          |   "contactName" : "TestContact Name",
          |   "email" : "test@email.com",
          |   "mobileNumber" : "0987654321",
          |   "phoneNumber" : "0987654321"
          | },
          | "newPAYEContactDetails" : {
          |   "contactName" : "TestContactName",
          |   "email" : "test@email.co.uk",
          |   "mobileNumber" : "1234567890",
          |   "phoneNumber" : "1234567890"
          | }
          |}
        """.stripMargin
      )

      Json.toJson(testExpectedJson) shouldBe testExpectedJson
    }
  }
}
