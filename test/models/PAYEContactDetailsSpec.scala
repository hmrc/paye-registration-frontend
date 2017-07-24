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

import models.view.PAYEContactDetails
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import testHelpers.PAYERegSpec

class PAYEContactDetailsSpec extends PAYERegSpec with JsonFormValidation {

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
      Json.fromJson[PAYEContactDetails](targetJsonMax) shouldBe JsSuccess(maxModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](maxModel) shouldBe targetJsonMax
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
      Json.fromJson[PAYEContactDetails](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](tstModel) shouldBe tstJson
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
      Json.fromJson[PAYEContactDetails](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](tstModel) shouldBe tstJson
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
      Json.fromJson[PAYEContactDetails](tstJson) shouldBe JsSuccess(tstModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](tstModel) shouldBe tstJson
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
      Json.fromJson[PAYEContactDetails](targetJsonMax)(PAYEContactDetails.prepopReads) shouldBe JsSuccess(maxModel)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](maxModel)(PAYEContactDetails.prepopWrites) shouldBe targetJsonMax
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
      Json.fromJson[PAYEContactDetails](json)(PAYEContactDetails.prepopReads) shouldBe JsSuccess(model)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](model)(PAYEContactDetails.prepopWrites) shouldBe json
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
      Json.fromJson[PAYEContactDetails](json)(PAYEContactDetails.prepopReads) shouldBe JsSuccess(model)
    }
    "write to Json" in {
      Json.toJson[PAYEContactDetails](model)(PAYEContactDetails.prepopWrites) shouldBe json
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

    val err = "No name components defined\n" +
      s"Lines defined:\n" +
      s"firstName: false\n" +
      s"middleName: false\n" +
      s"surname: false\n"

    "return an error when read from Json" in {
      val result = Json.fromJson[PAYEContactDetails](json)(PAYEContactDetails.prepopReads)
      shouldHaveErrors(result, JsPath(), Seq(ValidationError(err)))
    }
    "write to Json without firstName, middleName and surname" in {
      Json.toJson[PAYEContactDetails](model)(PAYEContactDetails.prepopWrites) shouldBe json
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
      shouldHaveErrors(result, JsPath(), Seq(ValidationError(err)))
    }
  }
}
