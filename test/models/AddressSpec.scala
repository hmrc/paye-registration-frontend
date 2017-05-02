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
import play.api.libs.json.{JsPath, JsSuccess, Json}
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

        val res = outcomeAddress(country = Some("UK"))

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
        shouldHaveErrors(result, JsPath(), Seq(ValidationError("neither string nor country were defined")))
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
    }
  }
}
