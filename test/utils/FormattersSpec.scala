/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import helpers.PayeComponentSpec
import models.Address
import play.api.libs.json._

class FormattersSpec extends PayeComponentSpec {

  val addr1 = Address(
    line1 = "line 1",
    line2 = "line 2",
    line3 = Some("line 3"),
    line4 = Some("line 4"),
    postCode = Some("TE1 1ST"),
    country = None,
    auditRef = Some("tstAuditRef")
  )

  val addr2 = Address(
    line1 = "line one",
    line2 = "line two",
    line3 = None,
    line4 = None,
    postCode = Some("TE1 2ST"),
    country = None
  )

  def testJsonWrite[V](map: Map[Int, V])(implicit formatV: Format[V]) =
    Json.toJson[Map[Int, V]](map)(Formatters.intMapWrites[V])

  def testJsonRead[V](json: JsValue)(implicit formatV: Format[V]) =
    Json.fromJson[Map[Int, V]](json)(Formatters.intMapReads[V])


  "MapWrites" should {
    "write a Map[Int, String]" in {
      val map = Map(1 -> "string1", 2 -> "string2")
      val json = Json.parse(
        s"""{
           |  "1":"string1",
           |  "2":"string2"
           |}
         """.stripMargin
      )

      testJsonWrite[String](map) mustBe json
    }

    "write a Map[Int, Address]" in {

      val map = Map(34 -> addr1, 12 -> addr2)
      val json = Json.parse(
        s"""{
           |  "34": {
           |    "line1":"line 1",
           |    "line2":"line 2",
           |    "line3":"line 3",
           |    "line4":"line 4",
           |    "postCode":"TE1 1ST",
           |    "auditRef":"tstAuditRef"
           |  },
           |  "12": {
           |    "line1":"line one",
           |    "line2":"line two",
           |    "postCode":"TE1 2ST"
           |  }
           |}
         """.stripMargin
      )

      testJsonWrite[Address](map) mustBe json
    }
  }

  "MapReads" should {
    "read a Map[Int, String]" in {
      val map = Map(1 -> "string1", 2 -> "string2")
      val json = Json.parse(
        s"""{
           |  "1":"string1",
           |  "2":"string2"
           |}
         """.stripMargin
      )

      testJsonRead[String](json).get mustBe map
    }

    "read a Map[Int, Address]" in {

      val map = Map(34 -> addr1, 12 -> addr2)
      val json = Json.parse(
        s"""{
           |  "34": {
           |    "line1":"line 1",
           |    "line2":"line 2",
           |    "line3":"line 3",
           |    "line4":"line 4",
           |    "postCode":"TE1 1ST",
           |    "auditRef":"tstAuditRef"
           |  },
           |  "12": {
           |    "line1":"line one",
           |    "line2":"line two",
           |    "postCode":"TE1 2ST"
           |  }
           |}
         """.stripMargin
      )

      testJsonRead[Address](json).get mustBe map
    }
  }

  "Strange strings" should {
    "be formatted so that the database can accept HMRC title, first, middle & last names" in {
      val testValue = JsString("""1234567890'af gh jghj g-æœhjg jg &@$£¥€ «»",.:;?!/\()[]{}<>*=#%+ÀÁÂÃÄÅĀĂĄÆǼÇĆĈĊČÞĎÐÈÉÊËĒĔĖĘĚĜĞĠĢĤĦÌÍÎÏĨĪĬĮİĴĶĹĻĽĿŁÑŃŅŇŊÒÓÔÕÖØŌŎŐǾŒŔŖŘŚŜŞŠŢŤŦÙÚÛÜŨŪŬŮŰŲŴẀẂẄỲÝŶŸŹŻŽÀÖØſƒǺǿẀẅỲỳ""")

      Json.fromJson(testValue)(Formatters.normalizeTrimmedHMRCReads) mustBe JsSuccess("1234567890'af gh jghj g-hjg jg  AAAAAAAAACCCCCDEEEEEEEEEGGGGHIIIIIIIIIJKLLLLNNNNOOOOOOOORRRSSSSTTUUUUUUUUUUWWWWYYYYZZZAOsAWwYy")
    }

    "be formatted so that the database can accept HMRC address lines" in {
      val testValue = JsString("""1234567890'af gh jghj g-æœhjg jg &@$£¥€ «»",.:;?!/\()[]{}<>*=#%+ÀÁÂÃÄÅĀĂĄÆǼÇĆĈĊČÞĎÐÈÉÊËĒĔĖĘĚĜĞĠĢĤĦÌÍÎÏĨĪĬĮİĴĶĹĻĽĿŁÑŃŅŇŊÒÓÔÕÖØŌŎŐǾŒŔŖŘŚŜŞŠŢŤŦÙÚÛÜŨŪŬŮŰŲŴẀẂẄỲÝŶŸŹŻŽÀÖØſƒǺǿẀẅỲỳ""")

      Json.fromJson(testValue)(Formatters.normalizeTrimmedHMRCAddressReads) mustBe JsSuccess("""1234567890'af gh jghj g-hjg jg & ",./\()AAAAAAAAACCCCCDEEEEEEEEEGGGGHIIIIIIIIIJKLLLLNNNNOOOOOOOORRRSSSSTTUUUUUUUUUUWWWWYYYYZZZAOsAWwYy""")
    }
  }
}
