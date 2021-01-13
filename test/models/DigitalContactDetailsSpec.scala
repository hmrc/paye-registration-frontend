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

package models

import helpers.PayeComponentSpec
import play.api.libs.json.{JsPath, JsSuccess, Json, JsonValidationError}

class DigitalContactDetailsSpec extends PayeComponentSpec {

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
      Json.fromJson[DigitalContactDetails](targetJsonMax) mustBe JsSuccess(maxModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](maxModel) mustBe targetJsonMax
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
      Json.fromJson[DigitalContactDetails](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](tstModel) mustBe tstJson
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
      Json.fromJson[DigitalContactDetails](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](tstModel) mustBe tstJson
    }
  }

  "BusinessContactDetails with minimal data (phone)" should {
    val tstJson = Json.parse(
      s"""{
         |  "phoneNumber":"0161385032"
         |}""".stripMargin)

    val tstModel = DigitalContactDetails(
      email = None,
      mobileNumber = None,
      phoneNumber = Some("0161385032")
    )


    "read from Json" in {
      Json.fromJson[DigitalContactDetails](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[DigitalContactDetails](tstModel) mustBe tstJson
    }
  }

  "BusinessContactDetails with full data to write Prepopulation Service" should {
    val targetJsonMax = Json.parse(
      s"""{
         |  "email":"test@email.com",
         |  "mobileNumber":"07943000111",
         |  "telephoneNumber":"0161385032"
         |}""".stripMargin)

    val maxModel = DigitalContactDetails(
      email = Some("test@email.com"),
      mobileNumber = Some("07943000111"),
      phoneNumber = Some("0161385032")
    )

    "write to Json" in {
      Json.toJson[DigitalContactDetails](maxModel)(DigitalContactDetails.prepopWrites) mustBe targetJsonMax
    }
  }

  "BusinessContactDetails with partial data no email to write Prepopulation Service" should {
    val json = Json.parse(
      s"""{
         |  "mobileNumber":"07943000111",
         |  "telephoneNumber":"0161385032"
         |}""".stripMargin)

    val model = DigitalContactDetails(
      email = None,
      mobileNumber = Some("07943000111"),
      phoneNumber = Some("0161385032")
    )

    "write to Json" in {
      Json.toJson[DigitalContactDetails](model)(DigitalContactDetails.prepopWrites) mustBe json
    }
  }

  "BusinessContactDetails with partial data no mobileNumber to write Prepopulation Service" should {
    val json = Json.parse(
      s"""{
         |  "email":"test@email.com",
         |  "telephoneNumber":"0161385032"
         |}""".stripMargin)

    val model = DigitalContactDetails(
      email = Some("test@email.com"),
      mobileNumber = None,
      phoneNumber = Some("0161385032")
    )

    "write to Json" in {
      Json.toJson[DigitalContactDetails](model)(DigitalContactDetails.prepopWrites) mustBe json
    }
  }

  "BusinessContactDetails with partial data no phoneNumber to write Prepopulation Service" should {
    val json = Json.parse(
      s"""{
         |  "email":"test@email.com",
         |  "mobileNumber":"07943000111"
         |}""".stripMargin)

    val model = DigitalContactDetails(
      email = Some("test@email.com"),
      mobileNumber = Some("07943000111"),
      phoneNumber = None
    )

    "write to Json" in {
      Json.toJson[DigitalContactDetails](model)(DigitalContactDetails.prepopWrites) mustBe json
    }
  }

  "BusinessContactDetails from Prepopulation Service" should {
    val err = "No digital contact details defined\n" +
      s"Lines defined:\n" +
      s"email: false\n" +
      s"mobile: false\n" +
      s"phone: false\n"

    "read successfully from Json" in {
      val targetJsonMax = Json.parse(
        s"""{
           |  "email":"test@email.com",
           |  "mobileNumber":"0794 300 01 11 45 67",
           |  "telephoneNumber":"0 16 13 85 03 20 98 76"
           |}""".stripMargin)

      val maxModel = DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = Some("079430001114567"),
        phoneNumber = Some("016138503209876")
      )

      Json.fromJson[DigitalContactDetails](targetJsonMax)(DigitalContactDetails.prepopReads) mustBe JsSuccess(maxModel)
    }

    "return an error when read from Json with no contact details" in {
      val json = Json.parse(s"""{}""".stripMargin)

      val result = Json.fromJson[DigitalContactDetails](json)(DigitalContactDetails.prepopReads)
      shouldHaveErrors(result, JsPath(), Seq(JsonValidationError(err)))
    }

    "return an error when read from Json with no valid phone number with less than 10 digits" in {
      val json = Json.parse(
        s"""{
           |  "mobileNumber": "343534098"
           |}""".stripMargin)

      val result = Json.fromJson[DigitalContactDetails](json)(DigitalContactDetails.prepopReads)
      shouldHaveErrors(result, JsPath(), Seq(JsonValidationError(err)))
    }

    "return an error when read from Json with no valid phone number with more than 20 digits" in {
      val json = Json.parse(
        s"""{
           |  "mobileNumber": "012345678901234567891"
           |}""".stripMargin)

      val result = Json.fromJson[DigitalContactDetails](json)(DigitalContactDetails.prepopReads)
      shouldHaveErrors(result, JsPath(), Seq(JsonValidationError(err)))
    }
  }
}
