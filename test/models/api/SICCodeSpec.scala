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
import play.api.libs.json.Json

class SICCodeSpec extends PayeComponentSpec {

  val testModel1 = SICCode(Some("12345"), Some("testDesc"))
  val testModel2 = SICCode(Some("23456"), Some("testDesc"))
  val testModel3 = SICCode(Some("34567"), Some("testDesc"))
  val testModel4 = SICCode(Some("45678"), Some("testDesc"))

  "A test model" should {
    "be formatted as json in the correct way" in {
      val result = Json.toJson(testModel1)
      val expected =
        Json.parse(
          """
            |{
            |   "code":"12345",
            |   "description":"testDesc"
            |}
          """.stripMargin)
      result mustBe expected
    }
  }

  "A sequence of SICCodes" should {
    "be formatted correctly" in {
      val sequence = Seq(testModel1, testModel2, testModel3, testModel4)
      val expected =
        Json.parse(
          """
            |[
            |   {
            |       "code":"12345",
            |       "description":"testDesc"
            |   },
            |   {
            |       "code":"23456",
            |       "description":"testDesc"
            |   },
            |   {
            |       "code":"34567",
            |       "description":"testDesc"
            |   },
            |   {
            |       "code":"45678",
            |       "description":"testDesc"
            |   }
            |]""".stripMargin)
      val result = Json.toJson(sequence)
      result mustBe expected
    }
  }
}
