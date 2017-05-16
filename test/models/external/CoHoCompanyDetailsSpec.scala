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

package models.external

import models.Address
import play.api.libs.json.{JsSuccess, Json}
import testHelpers.PAYERegSpec

class CoHoCompanyDetailsSpec extends PAYERegSpec {

  val tstModel = CoHoCompanyDetailsModel(
    companyName = "Test Company",
    roAddress = Address(
      "Line1",
      "Line2",
      None,
      None,
      Some("TE1 1ST")
    )
  )

  val tstJson = Json.parse(
    """{
      |  "company_name":"Test Company",
      |  "registered_office_address":{
      |    "line1":"Line1",
      |    "line2":"Line2",
      |    "postCode":"TE1 1ST"
      |  }
      |}""".stripMargin)

  "CoHoCompanyDetailsModel" should {
    "read from Json" in {
      Json.fromJson[CoHoCompanyDetailsModel](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[CoHoCompanyDetailsModel](tstModel) shouldBe tstJson
    }
  }

}
