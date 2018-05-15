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
import play.api.libs.json._

object IncorpUpdateResponse {

  def reads(txId: String, subscriber: String, regime: String): Reads[IncorporationStatus.Value] = new Reads[IncorporationStatus.Value] {
    override def reads(json: JsValue): JsResult[IncorporationStatus.Value] = {
      val incorpResponse = (json \ "SCRSIncorpStatus")
      val transactionId = (incorpResponse \ "IncorpSubscriptionKey" \ "transactionId").as[String]
      val sub           = (incorpResponse \ "IncorpSubscriptionKey" \ "subscriber").as[String]
      val reg           = (incorpResponse \ "IncorpSubscriptionKey" \ "discriminator").as[String]
      val status        = (incorpResponse \ "IncorpStatusEvent" \ "status").as[IncorporationStatus.Value]

      if (transactionId == txId && sub == subscriber && reg == regime) {
        JsSuccess(status)
      } else {
        JsError(s"[IncorpUpdateResponse] [reads] threw a match error using the following values response from II - txId: $transactionId, subscriber: $sub, regime: $reg, compared to actual values used - txId: $txId, subscriber: $subscriber, regime: $regime. IncorporationStatus received from II - $status")
      }
    }
  }
}