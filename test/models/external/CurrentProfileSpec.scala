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

package models.external

import helpers.PayeComponentSpec
import play.api.libs.json.{JsSuccess, Json}

class CurrentProfileSpec extends PayeComponentSpec {

  "Reading a CurrentProfile" should {
    "succeed" when {
      "status is defined" in {

        val tstCurrentProfile = CurrentProfile(
          registrationID = "54321",
          companyTaxRegistration = CompanyRegistrationProfile(
            status = "submitted",
            transactionId = "12345"
          ),
          language = "ENG",
          payeRegistrationSubmitted = true,
          None
        )

        val tstJson = Json.parse(
          """{
            |  "registrationID": "54321",
            |  "companyTaxRegistration": {
            |     "status": "submitted",
            |     "transactionId": "12345"
            |  },
            |  "language": "ENG",
            |  "payeRegistrationSubmitted": true
            |}""".stripMargin)

        Json.fromJson[CurrentProfile](tstJson) mustBe JsSuccess(tstCurrentProfile)
      }
    }

    "status is not defined" in {

      val tstCurrentProfile = CurrentProfile(
        registrationID = "54321",
        companyTaxRegistration = CompanyRegistrationProfile(
          status = "submitted",
          transactionId = "12345"
        ),
        language = "ENG",
        payeRegistrationSubmitted = false,
        None
      )

      val tstJson = Json.parse(
        """{
          |  "registrationID": "54321",
          |  "companyTaxRegistration": {
          |     "status": "submitted",
          |     "transactionId": "12345"
          |  },
          |  "language": "ENG"
          |}""".stripMargin)

      Json.fromJson[CurrentProfile](tstJson) mustBe JsSuccess(tstCurrentProfile)
    }
  }
}