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

package models.api

import java.time.LocalDate

import helpers.PayeComponentSpec
import play.api.libs.json.{JsSuccess, Json}

class EmploymentSpec extends PayeComponentSpec {


  "Employment" should {

    val testEmploymentMax = Fixtures.validEmploymentAPI

    val targetJsonMax = Json.parse(
      s"""{
          |  "employees":true,
          |  "ocpn":true,
          |  "cis":true,
          |  "first-payment-date":"${Fixtures.validDate}"
          |}""".stripMargin)

    "read from maximum Json" in {
      Json.fromJson[Employment](targetJsonMax) mustBe JsSuccess(testEmploymentMax)
    }

    "write to maximum Json" in {
      Json.toJson[Employment](testEmploymentMax) mustBe targetJsonMax
    }

    val testFutureDate = LocalDate.of(2016,12,20)
    val testFuturePayment = testFutureDate

    val testEmploymentMin = Fixtures.validEmploymentAPI.copy(
      employees = false,
      companyPension = None,
      subcontractors = false,
      firstPayDate = testFuturePayment
    )

    val targetJsonMin = Json.parse(
      s"""{
          |  "employees":false,
          |  "cis":false,
          |  "first-payment-date":"$testFutureDate"
          |}""".stripMargin)

    "read from minimum Json" in {
      Json.fromJson[Employment](targetJsonMin) mustBe JsSuccess(testEmploymentMin)
    }

    "write to minimum Json" in {
      Json.toJson[Employment](testEmploymentMin) mustBe targetJsonMin
    }
  }
}
