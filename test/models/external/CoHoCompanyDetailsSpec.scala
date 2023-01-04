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

package models.external

import helpers.PayeComponentSpec
import models.Address
import play.api.libs.json.{JsSuccess, Json}

class CoHoCompanyDetailsSpec extends PayeComponentSpec {

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
      Json.fromJson[CoHoCompanyDetailsModel](tstJson) mustBe JsSuccess(tstModel)
    }
    "read and normalize from Json" in {
      val tstJson2 = Json.parse(
        """{
          |  "company_name":"Tést Compàny",
          |  "registered_office_address":{
          |    "premises":"1",
          |    "address_line_1":"test stréèt",
          |    "locality":"Têstfôrd",
          |    "country":"ÜK",
          |    "postal_code":"TË2 2ST"
          |  }
          |}""".stripMargin)

      val tstModel2 = CoHoCompanyDetailsModel(
        companyName = "Tést Compàny",
        roAddress = Address(
          "1 test street",
          "Testford",
          None,
          None,
          Some("TE2 2ST")
        )
      )

      implicit val rds = CoHoCompanyDetailsModel.incorpInfoReads
      Json.fromJson[CoHoCompanyDetailsModel](tstJson2) mustBe JsSuccess(tstModel2)
    }

    "read and normalize from Json with address line 2" in {
      val tstJson2 = Json.parse(
        """{
          |  "company_name":"Tést Compàny",
          |  "registered_office_address":{
          |    "premises":"1",
          |    "address_line_1":"test stréèt. zone ÆĨ & O\\E",
          |    "address_line_2":"1/2 lane, (PO-BOX:; \"5')",
          |    "locality":"Têstfôrd",
          |    "country":"ÜK",
          |    "postal_code":"TË2 2ST"
          |  }
          |}""".stripMargin)

      val tstModel2 = CoHoCompanyDetailsModel(
        companyName = "Tést Compàny",
        roAddress = Address(
          "1",
          "test street. zone I & O\\E",
          Some("1/2 lane, (PO-BOX \"5')"),
          Some("Testford"),
          Some("TE2 2ST")
        )
      )

      implicit val rds = CoHoCompanyDetailsModel.incorpInfoReads
      Json.fromJson[CoHoCompanyDetailsModel](tstJson2) mustBe JsSuccess(tstModel2)
    }

    "write to Json" in {
      Json.toJson[CoHoCompanyDetailsModel](tstModel) mustBe tstJson
    }
  }

}
