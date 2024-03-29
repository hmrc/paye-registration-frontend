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

package models

import helpers.PayeComponentSpec
import models.view.PAYEContactDetails
import play.api.libs.json.{JsPath, JsSuccess, Json, JsonValidationError}

class PAYEContactDetailsSpec extends PayeComponentSpec {

  "PAYEContactDetails with full data" should {
    val targetJsonMax = Json.parse(
      s"""{
         |  "name":"tstName",
         |  "digitalContactDetails":{
         |    "email":"test@email.com",
         |    "mobileNumber":"07943000111",
         |    "phoneNumber":"0161385032"
         |  }
         |}""".stripMargin)

    val maxModel = PAYEContactDetails(
      "tstName",
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = Some("07943000111"),
        phoneNumber = Some("0161385032")
      )
    )

    "read from Json" in {
      Json.fromJson[PAYEContactDetails](targetJsonMax) mustBe JsSuccess(maxModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](maxModel) mustBe targetJsonMax
    }
  }

  "PAYEContactDetails with minimal data (name, email)" should {

    val tstJson = Json.parse(
      s"""{
         |  "name":"tstName",
         |  "digitalContactDetails":{
         |    "email":"test@email.com"
         |  }
         |}""".stripMargin)

    val tstModel = PAYEContactDetails(
      "tstName",
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = None,
        phoneNumber = None
      )
    )

    "read from Json" in {
      Json.fromJson[PAYEContactDetails](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](tstModel) mustBe tstJson
    }
  }

  "PAYEContactDetails with minimal data (name, mobile)" should {

    val tstJson = Json.parse(
      s"""{
         |  "name":"tstName",
         |  "digitalContactDetails":{
         |    "mobileNumber":"07943000111"
         |  }
         |}""".stripMargin)

    val tstModel = PAYEContactDetails(
      "tstName",
      DigitalContactDetails(
        email = None,
        mobileNumber = Some("07943000111"),
        phoneNumber = None
      )
    )

    "read from Json" in {
      Json.fromJson[PAYEContactDetails](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](tstModel) mustBe tstJson
    }
  }

  "PAYEContactDetails with minimal data (name, phone)" should {
    val tstJson = Json.parse(
      s"""{
         |  "name":"tstName",
         |  "digitalContactDetails":{
         |    "phoneNumber":"0161385032"
         |  }
         |}""".stripMargin)

    val tstModel = PAYEContactDetails(
      "tstName",
      DigitalContactDetails(
        email = None,
        mobileNumber = None,
        phoneNumber = Some("0161385032")
      )
    )


    "read from Json" in {
      Json.fromJson[PAYEContactDetails](tstJson) mustBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](tstModel) mustBe tstJson
    }
  }

  "PAYEContactDetails with full data from/to Prepopulation Service" should {
    val targetJsonMax = Json.parse(
      s"""{
         |  "firstName": "tstFirstName",
         |  "middleName": "tstMiddleName with multi space",
         |  "surname": "testSurname",
         |  "email": "test@email.com",
         |  "mobileNumber": "07943000111",
         |  "telephoneNumber": "0161385032"
         |}""".stripMargin)

    val maxModel = PAYEContactDetails(
      "tstFirstName tstMiddleName with multi space testSurname",
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = Some("07943000111"),
        phoneNumber = Some("0161385032")
      )
    )

    "read from Json" in {
      Json.fromJson[PAYEContactDetails](targetJsonMax)(PAYEContactDetails.prepopReads) mustBe JsSuccess(maxModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](maxModel)(PAYEContactDetails.prepopWrites) mustBe targetJsonMax
    }
  }

  "PAYEContactDetails with partial data from/to Prepopulation Service" should {
    val json = Json.parse(
      s"""{
         |  "firstName": "tstFirstName",
         |  "surname": "testSurname",
         |  "email": "test@email.com",
         |  "telephoneNumber": "0161385032"
         |}""".stripMargin)

    val model = PAYEContactDetails(
      "tstFirstName testSurname",
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = None,
        phoneNumber = Some("0161385032")
      )
    )

    "read from Json" in {
      Json.fromJson[PAYEContactDetails](json)(PAYEContactDetails.prepopReads) mustBe JsSuccess(model)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](model)(PAYEContactDetails.prepopWrites) mustBe json
    }
  }

  "PAYEContactDetails with partial name data from/to Prepopulation Service" should {
    val json = Json.parse(
      s"""{
         |  "firstName": "tstFirstName",
         |  "email": "test@email.com",
         |  "telephoneNumber": "0161385032"
         |}""".stripMargin)

    val model = PAYEContactDetails(
      "tstFirstName",
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = None,
        phoneNumber = Some("0161385032")
      )
    )

    "read from Json" in {
      Json.fromJson[PAYEContactDetails](json)(PAYEContactDetails.prepopReads) mustBe JsSuccess(model)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](model)(PAYEContactDetails.prepopWrites) mustBe json
    }
  }

  "PAYEContactDetails with noname from/to Prepopulation Service" should {
    val json = Json.parse(
      s"""{
         |  "email": "test@email.com",
         |  "telephoneNumber": "0161385032"
         |}""".stripMargin)

    val model = PAYEContactDetails(
      "",
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = None,
        phoneNumber = Some("0161385032")
      )
    )

    "return no errors as name is no longer sent by CRFE - when read from Json, set name to blank string" in {
      val result = Json.fromJson[PAYEContactDetails](json)(PAYEContactDetails.prepopReads)
      result mustBe JsSuccess(model)
    }
    "write to Json without firstName, middleName and surname" in {
      Json.toJson[PAYEContactDetails](model)(PAYEContactDetails.prepopWrites) mustBe json
    }
  }

  "PAYEContactDetails with no contact from/to Prepopulation Service" should {
    val targetJsonMax = Json.parse(
      s"""{
         |  "firstName": "tstFirstName"
         |}""".stripMargin)

    val err = "No digital contact details defined\n" +
      s"Lines defined:\n" +
      s"email: false\n" +
      s"mobile: false\n" +
      s"phone: false\n"

    "return an error when read from Json" in {
      val result = Json.fromJson[PAYEContactDetails](targetJsonMax)(PAYEContactDetails.prepopReads)
      shouldHaveErrors(result, JsPath(), Seq(JsonValidationError(err)))
    }
  }
}
