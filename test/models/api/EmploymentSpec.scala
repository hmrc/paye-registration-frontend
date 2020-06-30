/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsResultException, Json}

class EmploymentSpec extends PlaySpec {

  "creating an Employment case class from Json" should {
    "be successful" in {

      val json = Json.parse(
        """|{
           |   "employees": "alreadyEmploying",
           |   "firstPaymentDate": "2017-12-29",
           |   "construction": true,
           |   "subcontractors": true,
           |   "companyPension": true
           | }
        """.stripMargin).as[JsObject]

      val expectedModel = Employment(
        employees = Employing.alreadyEmploying,
        firstPaymentDate = LocalDate.of(2017, 12, 29),
        construction = true,
        subcontractors = true,
        companyPension = Some(true)
      )

      json.as[Employment] mustBe expectedModel
    }

    "be unsuccessful" in {
      val json = Json.parse(
        """|{
           |   "employees": "wrongValue",
           |   "firstPaymentDate": "2017-12-29",
           |   "construction": true,
           |   "subcontractors": true,
           |   "companyPension": true
           | }
        """.stripMargin).as[JsObject]

      a[JsResultException] mustBe thrownBy(json.as[Employment])
    }
  }
}