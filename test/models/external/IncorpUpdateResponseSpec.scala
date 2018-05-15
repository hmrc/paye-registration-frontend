/*
 * Copyright 2018 HM Revenue & Customs
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

package models.external

import enums.IncorporationStatus
import helpers.PayeComponentSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class IncorpUpdateResponseSpec extends PayeComponentSpec {
  "reads" should {
    val jsonResp = Json.parse(
      s"""
         |{
         |  "SCRSIncorpStatus": {
         |    "IncorpSubscriptionKey": {
         |      "subscriber": "testSub",
         |      "discriminator": "testRegime",
         |      "transactionId": "testTxId"
         |    },
         |    "SCRSIncorpSubscription": {
         |      "callbackUrl": "/callBackUrl"
         |    },
         |    "IncorpStatusEvent": {
         |      "status": "rejected",
         |      "crn": "12345678",
         |      "incorporationDate": "2017-04-25T16:20:10.000+01:00",
         |      "description": "test",
         |      "timestamp": "2017-04-25T16:20:10.000+01:00"
         |    }
         |  }
         |}
       """.stripMargin)

    "return successfully an enum IncorporationStatus value" in {
      val incorpStatus = Json.fromJson(jsonResp)(IncorpUpdateResponse.reads("testTxId", "testSub", "testRegime"))
      incorpStatus mustBe JsSuccess(IncorporationStatus.rejected)
    }

    "return an error" when {
      "the txId, subscriber and regime provided by II are not expected" in {
        val incorpStatus = Json.fromJson(jsonResp)(IncorpUpdateResponse.reads("diffTxId", "diffSub", "diffRegime"))
        incorpStatus.isError mustBe true
      }
    }
  }
}
