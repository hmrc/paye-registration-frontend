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

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsValue, JsPath, JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class AddressSpec extends UnitSpec with JsonFormValidation {
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
      Json.fromJson[Address](tstFullAddressJson) shouldBe JsSuccess(tstFullAddress)
    }
  }

  "Reading address from address-lookup-frontend" should {

    def outcomeAddress(line3: Option[String] = None,
                       line4: Option[String] = None,
                       country: Option[String] = None,
                       postcode: Option[String] = None
                        ) = Address(
                          "14 St Test Walker",
                          "Testford",
                          line3,
                          line4,
                          postcode,
                          country
                        )
    "succeed" when {
      "all lines are defined" in {
        val tstJson = Json.parse(
        """{
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

        val res = outcomeAddress(line3 = Some("Testley"), line4 = Some("Testshire"), postcode = Some("TE1 1ST"))

        Json.fromJson[Address](tstJson)(Address.adressLookupReads) shouldBe JsSuccess(res)
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

        Json.fromJson[Address](tstJson)(Address.adressLookupReads) shouldBe JsSuccess(res)
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

        Json.fromJson[Address](tstJson)(Address.adressLookupReads) shouldBe JsSuccess(res)
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

        Json.fromJson[Address](tstJson)(Address.adressLookupReads) shouldBe JsSuccess(res)
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

        Json.fromJson[Address](tstJson)(Address.adressLookupReads) shouldBe JsSuccess(res)
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

        Json.fromJson[Address](tstJson)(Address.adressLookupReads) shouldBe JsSuccess(res)
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

        Json.fromJson[Address](inputJson)(Address.adressLookupReads) shouldBe JsSuccess(expected)
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

        val result = Json.fromJson[Address](tstJson)(Address.adressLookupReads)
        shouldHaveErrors(result, JsPath(), Seq(ValidationError("No postcode and no country to default to")))
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

        val result = Json.fromJson[Address](tstJson)(Address.adressLookupReads)
        shouldHaveErrors(result, JsPath(), Seq(ValidationError("only 1 lines provided from address-lookup-frontend")))
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

        val result = Json.fromJson[Address](tstJson)(Address.adressLookupReads)
        shouldHaveErrors(result, JsPath(), Seq(ValidationError("Invalid postcode and no country to default to")))
      }
    }
  }

  "Reading Address from CoHo Json format" should {
    def readCoHoAddress(json: JsValue) = Json.fromJson[Address](json)(Address.incorpInfoReads)
    "succeed" when {

      "there is a long first line" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234",
            |  "address_line_1":"Really Long Street Name",
            |  "address_line_2":"Testley",
            |  "locality":"Testford",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST",
            |  "region":"Testshire"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("Testley"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a PO Box" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234",
            |  "po_box":"PO BOX TST36",
            |  "address_line_1":"Really Long Street Name",
            |  "locality":"Testford",
            |  "region":"Testshire",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("PO BOX TST36"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a PO Box and short address line 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "po_box":"PO BOX TST36",
            |  "address_line_1":"Short Street Name",
            |  "locality":"Testford",
            |  "region":"Testshire",
            |  "country":"UK"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "12 Short Street Name",
          line2 = "PO BOX TST36",
          line3 = Some("Testford"),
          line4 = Some("Testshire"),
          country = Some("UK"),
          postCode = None
        )

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a PO Box, a line 2 and short address line 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"12",
            |  "po_box":"PO BOX TST36",
            |  "address_line_1":"Short Street Name",
            |  "address_line_2":"Testley",
            |  "locality":"Testford",
            |  "region":"Testshire",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "12 Short Street Name",
          line2 = "Testley PO BOX TST36",
          line3 = Some("Testford"),
          line4 = Some("Testshire"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }

      "converting CHROAddress with a PO Box, a line 2 and long address line 1" in {
        val tstCHROAddressJson = Json.parse(
          """{
            |  "premises":"Unit 14234",
            |  "po_box":"PO BOX TST36",
            |  "address_line_1":"Really Long Street Name",
            |  "address_line_2":"Industrial estate",
            |  "po_box":"PO BOX TST36",
            |  "locality":"Testford",
            |  "region":"Testshire",
            |  "country":"UK",
            |  "postal_code":"TE1 1ST"
            |}""".stripMargin)

        val testAddress = Address(
          line1 = "Unit 14234",
          line2 = "Really Long Street Name",
          line3 = Some("Industrial estate PO BOX TST36"),
          line4 = Some("Testford"),
          country = None,
          postCode = Some("TE1 1ST")
        )

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }

      "converting CHROAddress with minimal data" in {
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

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }

      "converting and normalize CHROAddress with minimal data" in {
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

        readCoHoAddress(tstCHROAddressJson) shouldBe JsSuccess(testAddress)
      }
    }
  }
}
