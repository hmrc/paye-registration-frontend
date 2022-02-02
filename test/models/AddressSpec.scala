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

package models

import helpers.PayeComponentSpec
import play.api.libs.json._

class AddressSpec extends PayeComponentSpec {
  val tstFullAddress = Address(
    line1 = "14 St Test Walker",
    line2 = "Testford",
    line3 = Some("Testley"),
    line4 = Some("Testshire"),
    country = Some("UK"),
    postCode = Some("TE1 1ST")
  )

  val tstFullAddressJson = Json.parse(
    """{
      |  "line1":"14 St Test Walker",
      |  "line2":"Testford",
      |  "line3":"Testley",
      |  "line4":"Testshire",
      |  "country":"UK",
      |  "postCode":"TE1 1ST"
      |}""".stripMargin)

  "Address" should {
    "read from Json" in {
      Json.fromJson[Address](tstFullAddressJson) mustBe JsSuccess(tstFullAddress)
    }
  }

  "Reading address from address-lookup-frontend" should {

    def outcomeAddress(line3: Option[String] = None,
                       line4: Option[String] = None,
                       country: Option[String] = None,
                       postcode: Option[String] = None,
                       auditRef: Option[String] = None
                      ) = Address(
      "14 St Test Walker",
      "Testford",
      line3,
      line4,
      postcode,
      country,
      auditRef
    )

    "succeed" when {
      "all lines are defined" in {
        val tstJson = Json.parse(
          """{
            |  "auditRef":"tstAuditRef",
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford",
            |      "Testley",
            |      "Testshire"
            |    ],
            |    "postcode":"TE1 1ST",
            |    "country":{
            |      "code":"UK",
            |      "name":"United Kingdom"
            |    }
            |  }
            |}""".stripMargin)

        val res = outcomeAddress(line3 = Some("Testley"), line4 = Some("Testshire"), postcode = Some("TE1 1ST"), auditRef = Some("tstAuditRef"))

        Json.fromJson[Address](tstJson)(Address.addressLookupReads) mustBe JsSuccess(res)
      }
      "three lines are defined" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford",
            |      "Testley"
            |    ],
            |    "postcode":"TE1 1ST",
            |    "country":{
            |      "code":"UK",
            |      "name":"United Kingdom"
            |    }
            |  }
            |}""".stripMargin)

        val res = outcomeAddress(line3 = Some("Testley"), postcode = Some("TE1 1ST"))

        Json.fromJson[Address](tstJson)(Address.addressLookupReads) mustBe JsSuccess(res)
      }
      "two lines are defined" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford"
            |    ],
            |    "postcode":"TE1 1ST",
            |    "country":{
            |      "code":"UK",
            |      "name":"United Kingdom"
            |    }
            |  }
            |}""".stripMargin)

        val res = outcomeAddress(postcode = Some("TE1 1ST"))

        Json.fromJson[Address](tstJson)(Address.addressLookupReads) mustBe JsSuccess(res)
      }
      "postcode is not completed" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford"
            |    ],
            |    "country":{
            |      "code":"UK",
            |      "name":"United Kingdom"
            |    }
            |  }
            |}""".stripMargin)

        val res = outcomeAddress(country = Some("United Kingdom"))

        Json.fromJson[Address](tstJson)(Address.addressLookupReads) mustBe JsSuccess(res)
      }
      "country is not completed" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford"
            |    ],
            |    "postcode":"TE1 1ST"
            |  }
            |}""".stripMargin)

        val res = outcomeAddress(postcode = Some("TE1 1ST"))

        Json.fromJson[Address](tstJson)(Address.addressLookupReads) mustBe JsSuccess(res)
      }
      "postcode is invalid but country is completed" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford"
            |    ],
            |    "postcode":"Inval!d P0STCODE",
            |    "country":{
            |      "code":"UK",
            |      "name":"United Kingdom"
            |    }
            |  }
            |}""".stripMargin)

        val res = outcomeAddress(country = Some("United Kingdom"))

        Json.fromJson[Address](tstJson)(Address.addressLookupReads) mustBe JsSuccess(res)
      }

      "lines 1-4 are too long and are there trimmed" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     "abcdefghijklmnopqrstuvwxyz@#",
            |     "abcdefghijklmnopqrstuvwxyz@#",
            |     "abcdefghijklmnopqrstuvwxyz@#",
            |     "abcdefghijklmnopqrstuvwxyz@#"
            |   ],
            |   "postcode" : "TF4 2FT",
            |   "country" : {
            |     "code" : "UK",
            |     "name" : "United Kingdom"
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "abcdefghijklmnopqrstuvwxyz@",
          line2 = "abcdefghijklmnopqrstuvwxyz@",
          line3 = Some("abcdefghijklmnopqrstuvwxyz@"),
          line4 = Some("abcdefghijklmnopqr"),
          postCode = Some("TF4 2FT"),
          country = None
        )

        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }

      "there are trailing spaces in UK address" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     "   14 Saint John Walker",
            |     "   Telford  "
            |   ],
            |   "postcode" : " TF4 2FT ",
            |   "country" : {
            |     "code" : "UK",
            |     "name" : " United Kingdom "
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "14 Saint John Walker",
          line2 = "Telford",
          line3 = None,
          line4 = None,
          postCode = Some("TF4 2FT"),
          country = None
        )

        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }

      "construct a foreign address with the postcode in address line 3" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     "line 1",
            |     "line 2"
            |   ],
            |   "postcode" : "0121",
            |   "country" : {
            |     "code" : "USA",
            |     "name" : "United States of America"
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "line 1",
          line2 = "line 2",
          line3 = Some("0121"),
          line4 = None,
          postCode = None,
          country = Some("United States of America")
        )

        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }

      "construct a foreign address with the postcode in address line 4" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     "line 1",
            |     "line 2",
            |     "line 3"
            |   ],
            |   "postcode" : "0121",
            |   "country" : {
            |     "code" : "USA",
            |     "name" : "United States of America"
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "line 1",
          line2 = "line 2",
          line3 = Some("line 3"),
          line4 = Some("0121"),
          postCode = None,
          country = Some("United States of America")
        )

        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }

      "construct a foreign address and remove trailing spaces" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     " line 1 ",
            |     " line 2 "
            |   ],
            |   "postcode" : " 0121 ",
            |   "country" : {
            |     "code" : "USA",
            |     "name" : " United States of America "
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "line 1",
          line2 = "line 2",
          line3 = Some("0121"),
          line4 = None,
          postCode = None,
          country = Some("United States of America")
        )

        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }
    }
    "Succeed AND normalise" when {
      "All lines are too long and contain special chars with every field populated, With a country of United Kingdom, country set to None" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     "æÆœŒßØûabcdefghijklmnopqrstuvwxyz@#",
            |     "æÆœŒßØûabcdefghijklmnopqrstuvwxyz@#",
            |     "æÆœŒßØûabcdefghijklmnopqrstuvwxyz@#",
            |     "æÆœŒßØûabcdefghijklmnopqrstuvwxyz@#"
            |   ],
            |   "postcode" : "Æ4 2Æ",
            |   "country" : {
            |     "code" : "UK",
            |     "name" : "United Kingdom"
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "aeAEoeOEssOuabcdefghijklmno",
          line2 = "aeAEoeOEssOuabcdefghijklmno",
          line3 = Some("aeAEoeOEssOuabcdefghijklmno"),
          line4 = Some("aeAEoeOEssOuabcdef"),
          postCode = Some("AE4 2AE"),
          country = None
        )
        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }
      "max limit is provided, special characters normalised push limit over, address trimmed, country not set to None" in {
        val inputJson = Json.parse(
          """
            |{
            | "address" : {
            |   "lines": [
            |     "abc27charsabcdfedgehdjtretæ",
            |     "abc27charsabcdfedgehdjtretæ",
            |     "abc27charsabcdfedgehdjtretæ",
            |     "abc18chars78hshdgæ"
            |   ],
            |   "postcode" : "Æ42ÆÆ42ÆÆ42ÆÆ42Æ",
            |   "country" : {
            |     "code" : "UK",
            |     "name" : "æabcdefghijklmnopqrstuvwxyz@#"
            |   }
            | }
            |}
          """.stripMargin)

        val expected = Address(
          line1 = "abc27charsabcdfedgehdjtreta",
          line2 = "abc27charsabcdfedgehdjtreta",
          line3 = Some("abc27charsabcdfedgehdjtreta"),
          line4 = Some("abc18chars78hshdga"),
          postCode = None,
          country = Some("aeabcdefghijklmnopqrstuvwxyz@#")
        )
        Json.fromJson[Address](inputJson)(Address.addressLookupReads) mustBe JsSuccess(expected)
      }
    }

    "fail" when {
      "neither postcode nor country are completed" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford"
            |    ]
            |  }
            |}""".stripMargin)

        val result = Json.fromJson[Address](tstJson)(Address.addressLookupReads)
        shouldHaveErrors(result, JsPath(), Seq(JsonValidationError("No postcode and no country to default to")))
      }
      "only one address line is completed" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker"
            |    ],
            |    "postcode":"TE1 1ST"
            |  }
            |}""".stripMargin)

        val result = Json.fromJson[Address](tstJson)(Address.addressLookupReads)
        shouldHaveErrors(result, JsPath(), Seq(JsonValidationError("only 1 lines provided from address-lookup-frontend")))
      }
      "postcode is invalid and country is not completed" in {
        val tstJson = Json.parse(
          """{
            |  "address":{
            |    "lines":[
            |      "14 St Test Walker",
            |      "Testford"
            |    ],
            |    "postcode":"Inval!d P0STCODE"
            |  }
            |}""".stripMargin)

        val result = Json.fromJson[Address](tstJson)(Address.addressLookupReads)
        shouldHaveErrors(result, JsPath(), Seq(JsonValidationError("Invalid postcode and no country to default to")))
      }
    }
  }


  "Reading Address from CoHo Json format" should {
    def readCoHoAddress(json: JsValue) = Json.fromJson[Address](json)(Address.incorpInfoReads)

    "succeed" when {

      "removes characters that would normally split out into 2 (e.g. Æ -> AE)" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234æ",
            |  "address_line_1":"ReallyÆ Long Street Name",
            |  "address_line_2":"TØesœtley",
            |  "locality":"TestŒford",
            |  "country":"UKß",
            |  "postal_code":"TE1 ø1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("Testley"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "there is a long first line" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234",
            |  "address_line_1":"Really Long Street Name",
            |  "address_line_2":"Testley",
            |  "locality":"Testford",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("Testley"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting CHROAddress without a premises" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "address_line_1":"1 Really Long Street Name",
            |  "locality":"Testford",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "1 Really Long Street Name",
          line2 = "Testford",
          line3 = None,
          line4 = None,
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a short address line 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "address_line_1":"Short Street Name",
            |  "locality":"Testford",
            |  "country":"UK"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "12 Short Street Name",
          line2 = "Testford",
          line3 = None,
          line4 = None,
          country = Some("UK"),
          postCode = None
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a line 2 and short address line 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "address_line_1":"Short Street Name",
            |  "address_line_2":"Testley",
            |  "locality":"Testford",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "12 Short Street Name",
          line2 = "Testley",
          line3 = Some("Testford"),
          line4 = None,
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a line 2 and long address line 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234",
            |  "address_line_1":"Really Long Street Name",
            |  "address_line_2":"Industrial estate",
            |  "locality":"Testford",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("Industrial estate"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a line 2 and long address line 1 with trailing spaces" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"  Unit 14234  ",
            |  "address_line_1":" Really Long Street Name    ",
            |  "address_line_2":"     Industrial estate  ",
            |  "locality":"    Testford ",
            |  "country":" UK ",
            |  "postal_code":"  TE1 1ST  "
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("Industrial estate"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting CHROAddress with minimal data and premises" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "address_line_1":"Short Street Name",
            |  "locality":"Testford",
            |  "postal_code":"TE1 1ST",
            |  "country":"UK"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "12 Short Street Name",
          line2 = "Testford",
          line3 = None,
          line4 = None,
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }

      "converting and normalize CHROAddress with minimal data and premises" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "address_line_1":"Shört Stréèt Name",
            |  "locality":"Têstfôrd",
            |  "postal_code":"TË1 1ST",
            |  "country":"ÜK"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "12 Short Street Name",
          line2 = "Testford",
          line3 = None,
          line4 = None,
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) mustBe JsSuccess(testAddress)
      }
    }

    "fail" when {
      "there are not enough lines defined 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "postal_code":"TE1 1ST",
            |  "country":"UK"
            |}""".stripMargin)

        val res: JsResult[Address] = readCoHoAddress(tstCHROAddressJson)
        val err = "Only 1 address lines returned from II for RO Address\n" +
          "Lines defined:\n" +
          "premises: true\n" +
          "address line 1: false\n" +
          "address line 2: false\n" +
          "locality: false\n" +
          "postcode: true\n" +
          "country: true\n"
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError(err)))
      }
      "there are not enough lines defined 2" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "postal_code":"TE1 1ST",
            |  "country":"UK"
            |}""".stripMargin)

        val res: JsResult[Address] = readCoHoAddress(tstCHROAddressJson)
        val err = "Only 0 address lines returned from II for RO Address\n" +
          "Lines defined:\n" +
          "premises: false\n" +
          "address line 1: false\n" +
          "address line 2: false\n" +
          "locality: false\n" +
          "postcode: true\n" +
          "country: true\n"
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError(err)))
      }
      "there are not enough lines defined 3" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "address_line_1":"Short Street Name",
            |  "postal_code":"TE1 1ST",
            |  "country":"UK"
            |}""".stripMargin)

        val res: JsResult[Address] = readCoHoAddress(tstCHROAddressJson)
        val err = "Only 1 address lines returned from II for RO Address\n" +
          "Lines defined:\n" +
          "premises: false\n" +
          "address line 1: true\n" +
          "address line 2: false\n" +
          "locality: false\n" +
          "postcode: true\n" +
          "country: true\n"
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError(err)))
      }
      "there are not enough lines defined 4" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "address_line_2":"Short Street Name",
            |  "postal_code":"TE1 1ST",
            |  "country":"UK"
            |}""".stripMargin)

        val res: JsResult[Address] = readCoHoAddress(tstCHROAddressJson)
        val err = "Only 1 address lines returned from II for RO Address\n" +
          "Lines defined:\n" +
          "premises: false\n" +
          "address line 1: false\n" +
          "address line 2: true\n" +
          "locality: false\n" +
          "postcode: true\n" +
          "country: true\n"
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError(err)))
      }
      "there are not enough lines defined 5" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "locality":"Short Street Name",
            |  "postal_code":"TE1 1ST",
            |  "country":"UK"
            |}""".stripMargin)

        val res: JsResult[Address] = readCoHoAddress(tstCHROAddressJson)
        val err = "Only 1 address lines returned from II for RO Address\n" +
          "Lines defined:\n" +
          "premises: false\n" +
          "address line 1: false\n" +
          "address line 2: false\n" +
          "locality: true\n" +
          "postcode: true\n" +
          "country: true\n"
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError(err)))
      }
      "there is neither postcode nor country defined" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234",
            |  "address_line_1":"Really Long Street Name",
            |  "address_line_2":"Testley",
            |  "locality":"Testford"
            |}""".stripMargin)

        val res: JsResult[Address] = readCoHoAddress(tstCHROAddressJson)
        val err = "Neither postcode nor country returned from II for RO Address\n" +
          "Lines defined:\n" +
          "premises: true\n" +
          "address line 1: true\n" +
          "address line 2: true\n" +
          "locality: true\n" +
          "postcode: false\n" +
          "country: false\n"
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError(err)))
      }
    }

    "Reading Address from PrePop Json format" should {
      def readPrePopAddress(json: JsValue) = Json.fromJson[Address](json)(Address.prePopFormat)

      "succeed" when {
        "All lines are defined" in {
          val json = Json.parse(
            """{
              |  "addressLine1":"Line 1",
              |  "addressLine2":"Line 2",
              |  "addressLine3":"Line 3",
              |  "addressLine4":"Line 4",
              |  "country":"UK",
              |  "postcode":"TE1 1ST",
              |  "auditRef":"tstAuditRef"
              |}
          """.stripMargin
          )
          val addr = Address(
            line1 = "Line 1",
            line2 = "Line 2",
            line3 = Some("Line 3"),
            line4 = Some("Line 4"),
            country = None,
            postCode = Some("TE1 1ST"),
            auditRef = Some("tstAuditRef")
          )
          readPrePopAddress(json) mustBe JsSuccess(addr)
        }
        "First two lines and country are defined" in {
          val json = Json.parse(
            """{
              |  "addressLine1":"Line 1",
              |  "addressLine2":"Line 2",
              |  "country":"UK"
              |}
            """.stripMargin
          )
          val addr = Address(
            line1 = "Line 1",
            line2 = "Line 2",
            line3 = None,
            line4 = None,
            country = Some("UK"),
            postCode = None
          )
          readPrePopAddress(json) mustBe JsSuccess(addr)
        }
        "Postcode is invalid but country is defined" in {
          val json = Json.parse(
            """{
              |  "addressLine1":"Line 1",
              |  "addressLine2":"Line 2",
              |  "country":"UK",
              |  "postcode":"INVALID POSTCODE"
              |}
            """.stripMargin
          )
          val addr = Address(
            line1 = "Line 1",
            line2 = "Line 2",
            line3 = None,
            line4 = None,
            country = Some("UK"),
            postCode = None
          )
          readPrePopAddress(json) mustBe JsSuccess(addr)
        }
      }
      "fail" when {
        "Not enough are defined" in {
          val json = Json.parse(
            """{
              |  "addressLine1":"Line 1",
              |  "country":"UK",
              |  "postcode":"TE1 1ST"
              |}
            """.stripMargin
          )
          intercept[JsResultException](readPrePopAddress(json))
        }
      }
      "Neither postcode nor country are defined" in {
        val json = Json.parse(
          """{
            |  "addressLine1":"Line 1",
            |  "addressLine2":"Line 2"
            |}
          """.stripMargin
        )
        val res = readPrePopAddress(json)
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError("Neither country nor valid postcode defined in PrePop Address")))
      }
      "Postcode is invalid and no country defined" in {
        val json = Json.parse(
          """{
            |  "addressLine1":"Line 1",
            |  "addressLine2":"Line 2",
            |  "postcode":"NotAPostcode"
            |}
          """.stripMargin
        )
        val res = readPrePopAddress(json)
        shouldHaveErrors(res, JsPath(), Seq(JsonValidationError("Neither country nor valid postcode defined in PrePop Address")))
      }
    }

    "Writing Address to PrePop Json format" should {
      def writePrePopAddress(addr: Address) = Json.toJson[Address](addr)(Address.prePopWrites)

      "succeed" when {
        "All lines are defined" in {
          val json = Json.parse(
            """{
              |  "addressLine1":"Line 1",
              |  "addressLine2":"Line 2",
              |  "addressLine3":"Line 3",
              |  "addressLine4":"Line 4",
              |  "postcode":"TE1 1ST",
              |  "auditRef":"tstAuditRef"
              |}
            """.stripMargin
          )
          val addr = Address(
            line1 = "Line 1",
            line2 = "Line 2",
            line3 = Some("Line 3"),
            line4 = Some("Line 4"),
            country = None,
            postCode = Some("TE1 1ST"),
            auditRef = Some("tstAuditRef")
          )
          writePrePopAddress(addr) mustBe json
        }
        "First two lines and country are defined" in {
          val json = Json.parse(
            """{
              |  "addressLine1":"Line 1",
              |  "addressLine2":"Line 2",
              |  "country":"UK"
              |}
            """.stripMargin
          )
          val addr = Address(
            line1 = "Line 1",
            line2 = "Line 2",
            line3 = None,
            line4 = None,
            country = Some("UK"),
            postCode = None
          )
          writePrePopAddress(addr) mustBe json
        }
      }
    }
  }
}
