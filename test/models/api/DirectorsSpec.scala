/*
 * Copyright 2022 HM Revenue & Customs
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

import helpers.PayeComponentSpec
import play.api.libs.json.{JsSuccess, Json}

class DirectorsSpec extends PayeComponentSpec {

  "Name" should {

    val tstJson = Json.parse(
      s"""{
         |  "forename":"Timothy",
         |  "other_forenames":"Potterley-Smythe",
         |  "surname":"Buttersford",
         |  "title":"Mr"
         |}""".stripMargin)

    val tstJsonTitle = Json.parse(
      s"""{
         |  "forename":"Timothy",
         |  "other_forenames":"Potterley-Smythe",
         |  "surname":"Buttersford",
         |  "title":"Brigadier Bridge Buddies"
         |}""".stripMargin)


    val tstJsonWithCommas = Json.parse(
      s"""{
         |  "forename":"Timo,,thy",
         |  "other_forenames":"Potterl,ey-Smythe",
         |  "surname":"Buttersf,ord",
         |  "title":"Brigadier Br,idge Budd.ies"
         |}""".stripMargin)

    val tstModel = Name(
      forename = Some("Timothy"),
      otherForenames = Some("Potterley-Smythe"),
      surname = "Buttersford",
      title = Some("Mr")
    )

    val tstModelTitle = Name(
      forename = Some("Timothy"),
      otherForenames = Some("Potterley-Smythe"),
      surname = "Buttersford",
      title = None
    )

    "read from json with full data" in {
      Json.fromJson[Name](tstJson) mustBe JsSuccess(tstModel)
    }
    "read from json with a title over 20 characters" in {
      Json.fromJson[Name](tstJsonTitle)(Name.normalizeNameReads) mustBe JsSuccess(tstModelTitle)
    }
    "read from json with commas" in {
      Json.fromJson[Name](tstJsonWithCommas)(Name.normalizeNameReads) mustBe JsSuccess(tstModelTitle)
    }
    "write to json with full data" in {
      Json.toJson[Name](tstModel) mustBe tstJson
    }

    val tstEmptyJson = Json.parse(s"""{"surname":""}""".stripMargin)
    val tstEmptyModel = Name(None, None, "", None)

    "read from json with empty data" in {
      Json.fromJson[Name](tstEmptyJson) mustBe JsSuccess(tstEmptyModel)
    }
    "write to json with empty data" in {
      Json.toJson[Name](tstEmptyModel) mustBe tstEmptyJson
    }

  }

  "Director" should {

    val tstJson = Json.parse(
      s"""{
         |  "nino":"ZZ123456A",
         |  "director":{
         |    "forename":"Timothy",
         |    "other_forenames":"Potterley-Smythe",
         |    "surname":"Buttersford",
         |    "title":"Mr"
         |  }
         |}""".stripMargin)

    val tstModel = Director(
      name = Name(
        forename = Some("Timothy"),
        otherForenames = Some("Potterley-Smythe"),
        surname = "Buttersford",
        title = Some("Mr")
      ),
      nino = Some("ZZ123456A")
    )

    "read from json with full data" in {
      Json.fromJson[Director](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to json with full data" in {
      Json.toJson[Director](tstModel) mustBe tstJson
    }


    val tstEmptyJson = Json.parse(
      s"""{
         |  "director":{"surname":""}
         |}""".stripMargin)

    val tstEmptyModel = Director(
      name = Name(
        forename = None,
        otherForenames = None,
        surname = "",
        title = None
      ),
      nino = None
    )

    "read from json with minimal data" in {
      Json.fromJson[Director](tstEmptyJson) mustBe JsSuccess(tstEmptyModel)
    }
    "write to json with minimal data" in {
      Json.toJson[Director](tstEmptyModel) mustBe tstEmptyJson
    }
  }
}
