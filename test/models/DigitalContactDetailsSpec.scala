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

package models

import play.api.libs.json.{JsSuccess, Json}
import testHelpers.PAYERegSpec

class DigitalContactDetailsSpec extends PAYERegSpec {

  "BusinessContactDetails with full data" should {
    val targetJsonMax = Json.parse(
      s"""{
         |  "email":"test@email.com",
         |  "mobileNumber":"07943000111",
         |  "phoneNumber":"0161385032"
         |}""".stripMargin)

    val maxModel = DigitalContactDetails(
      email = Some("test@email.com"),
      mobileNumber = Some("07943000111"),
      phoneNumber = Some("0161385032")
    )
    "read from Json" in {
      Json.fromJson[DigitalContactDetails](targetJsonMax) shouldBe JsSuccess(maxModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](maxModel) shouldBe targetJsonMax
    }
  }

  "BusinessContactDetails with minimal data (email)" should {

    val tstJson = Json.parse(
      s"""{
         |  "email":"test@email.com"
         |}""".stripMargin)

    val tstModel = DigitalContactDetails(
      email = Some("test@email.com"),
      mobileNumber = None,
      phoneNumber = None
    )

    "read from Json" in {
      Json.fromJson[DigitalContactDetails](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](tstModel) shouldBe tstJson
    }
  }

  "BusinessContactDetails with minimal data (mobile)" should {

    val tstJson = Json.parse(
      s"""{
         |  "mobileNumber":"07943000111"
         |}""".stripMargin)

    val tstModel = DigitalContactDetails(
      email = None,
      mobileNumber = Some("07943000111"),
      phoneNumber = None
    )

    "read from Json" in {
      Json.fromJson[DigitalContactDetails](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](tstModel) shouldBe tstJson
    }
  }

  "BusinessContactDetails with minimal data (phone)" should {
    val tstJson = Json.parse(
      s"""{
         |  "phoneNumber":"0161385032"
         |}""".stripMargin)

    val tstModel = DigitalContactDetails (
      email = None,
      mobileNumber = None,
      phoneNumber = Some("0161385032")
    )


    "read from Json" in {
      Json.fromJson[DigitalContactDetails](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](tstModel) shouldBe tstJson
    }
  }

}
