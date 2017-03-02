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
import uk.gov.hmrc.play.test.UnitSpec

class AddressSpec extends UnitSpec {
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
}
