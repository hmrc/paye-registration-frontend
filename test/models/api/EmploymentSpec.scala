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

package models.api

import java.time.LocalDate

import play.api.libs.json.{JsSuccess, Json}
import testHelpers.PAYERegSpec

class EmploymentSpec extends PAYERegSpec {


  "Employment" should {

    val testPastDate = LocalDate.of(2016,12,20)
    val testPastPayment = FirstPayment(paymentMade = true, firstPayDate = testPastDate)

    val testEmploymentMax = Employment(employees = true,
      companyPension = Some(true),
      subcontractors = true,
      firstPayment = testPastPayment)

    val targetJsonMax = Json.parse(
      s"""{
          |  "employees":true,
          |  "companyPension":true,
          |  "subcontractors":true,
          |  "firstPayment":{
          |     "paymentMade":true,
          |     "firstPayDate":"$testPastDate"
          |  }
          |}""".stripMargin)

    "read from maximum Json" in {
      Json.fromJson[Employment](targetJsonMax) shouldBe JsSuccess(testEmploymentMax)
    }

    "write to maximum Json" in {
      Json.toJson[Employment](testEmploymentMax) shouldBe targetJsonMax
    }

    val testFutureDate = LocalDate.of(2016,12,20)
    val testFuturePayment = FirstPayment(paymentMade = false, firstPayDate = testFutureDate)

    val testEmploymentMin = Employment(employees = false,
      companyPension = None,
      subcontractors = false,
      firstPayment = testFuturePayment)

    val targetJsonMin = Json.parse(
      s"""{
          |  "employees":false,
          |  "subcontractors":false,
          |  "firstPayment":{
          |     "paymentMade":false,
          |     "firstPayDate":"$testFutureDate"
          |  }
          |}""".stripMargin)

    "read from minimum Json" in {
      Json.fromJson[Employment](targetJsonMin) shouldBe JsSuccess(testEmploymentMin)
    }

    "write to minimum Json" in {
      Json.toJson[Employment](testEmploymentMin) shouldBe targetJsonMin
    }
  }

}
