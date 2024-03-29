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

package models.view

import helpers.PayeComponentSpec
import models.api.{Director, Name}
import play.api.libs.json.Json

class DirectorsSpec extends PayeComponentSpec {

  "Ninos View Model" should {

    val tstModel = Ninos(
      List(
        UserEnteredNino("0", Some("ZZ123456A")),
        UserEnteredNino("1", Some("ZZ223456A"))
      )
    )
    val tstJson = Json.parse(
      s"""{
         |"ninoMapping":[
         |  {"id":"0","nino":"ZZ123456A"},
         |  {"id":"1","nino":"ZZ223456A"}
         |  ]
         |}""".stripMargin)

    "read from json with full data" in {
      Json.fromJson[Ninos](tstJson).asOpt mustBe Some(tstModel)
    }
    "write to json with full data" in {
      Json.toJson[Ninos](tstModel) mustBe tstJson
    }

    val tstPartialModel = Ninos(
      ninoMapping = List(
        UserEnteredNino("0", Some("ZZ123456A")),
        UserEnteredNino("1", None)
      )
    )
    val tstPartialJson = Json.parse(
      s"""{
         |"ninoMapping":[
         |  {"id":"0","nino":"ZZ123456A"},
         |  {"id":"1"}
         |  ]
         |}""".stripMargin)

    "read from json with partial data" in {
      Json.fromJson[Ninos](tstPartialJson).asOpt mustBe Some(tstPartialModel)
    }
    "write to json with partial data" in {
      Json.toJson[Ninos](tstPartialModel) mustBe tstPartialJson
    }

    val tstEmptyModel = Ninos(
      ninoMapping = List.empty
    )
    val tstEmptyJson = Json.parse(
      s"""{
         |"ninoMapping":[]
         |}""".stripMargin)

    "read from json with empty data" in {
      Json.fromJson[Ninos](tstEmptyJson).asOpt mustBe Some(tstEmptyModel)
    }
    "write to json with empty data" in {
      Json.toJson[Ninos](tstEmptyModel) mustBe tstEmptyJson
    }
  }

  "Directors View Model" should {

    val tstModel = Directors(Map("1" -> Director(name = Name(None, None, Some("Nathan"), None), nino = Some("ZZ123456A"))))
    val tstJson = Json.parse(
      s"""{
         |  "directorMapping":{
         |    "1":{
         |      "director":{
         |        "surname":"Nathan"
         |      },
         |    "nino":"ZZ123456A"
         |    }
         |  }
         |}""".stripMargin)

    "read from json" in {
      Json.fromJson[Directors](tstJson).asOpt mustBe Some(tstModel)
    }
    "write to json" in {
      Json.toJson[Directors](tstModel) mustBe tstJson
    }
  }


}
