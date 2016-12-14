/*
 * Copyright 2016 HM Revenue & Customs
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

package models

import helpers.PAYERegSpec
import models.coHo.{AreaOfIndustry, CoHoCompanyDetailsModel}
import play.api.libs.json.Json

class CoHoCompanyDetailsSpec extends PAYERegSpec {

  val tstModel = CoHoCompanyDetailsModel(
    registrationID = "12345",
    companyName = "Test Company",
    areasOfIndustry = Seq(
      AreaOfIndustry(
        sicCode = "100",
        description = "Chips"
      ),
      AreaOfIndustry(
        sicCode = "101",
        description = "Fish"
      )
    )
  )

  val tstJson = Json.parse(
    """{
      |  "registration_id":"12345",
      |  "company_name":"Test Company",
      |  "areas_of_industry": [
      |    {
      |      "sic_code":"100",
      |      "description":"Chips"
      |    },
      |    {
      |      "sic_code":"101",
      |      "description":"Fish"
      |    }
      |  ]
      |}""".stripMargin)

  "CoHoCompanyDetailsModel" should {
    "read from Json" in {
      Json.fromJson[CoHoCompanyDetailsModel](tstJson).asOpt shouldBe Some(tstModel)
    }
    "write to Json" in {
      Json.toJson[CoHoCompanyDetailsModel](tstModel) shouldBe tstJson
    }
  }

}
