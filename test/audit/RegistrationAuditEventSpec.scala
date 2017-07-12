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

package audit

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.play.test.UnitSpec

class RegistrationAuditEventSpec extends UnitSpec {

  "RegistrationEvent" should {
    val clientIP: String = "1.2.3.4"
    val clientPort: String = "1234"
    val auditType = "testType"
    val bearer = "Bearer 12345"
    val session: String = "sess"
    val request: String = "req"
    val device = "device"

    val completeCarrier = HeaderCarrier(
      trueClientIp = Some(clientIP),
      trueClientPort = Some(clientPort),
      forwarded = Some(ForwardedFor("2.3.4.5")),
      sessionId = Some(SessionId(session)),
      requestId = Some(RequestId(request)),
      authorization = Some(Authorization(bearer)),
      deviceID = Some(device)
    )

    val emptyCarrier = HeaderCarrier()

    "have the correct tags for a full header carrier" in {
      val event = new RegistrationAuditEvent(auditType, None, Json.obj())(completeCarrier) {}

      val result = Json.toJson[ExtendedDataEvent](event)

      val expectedTags: JsObject = Json.obj(
        "clientIP"        -> clientIP,
        "clientPort"      -> clientPort,
        "X-Request-ID"    -> request,
        "X-Session-ID"    -> session,
        "transactionName" -> auditType,
        "deviceID"        -> device,
        "Authorization"   -> bearer
      )

      (result \ "tags").as[JsObject] shouldBe expectedTags
    }

    "have the correct tags for an empty header carrier" in {
      val event = new RegistrationAuditEvent(auditType, None, Json.obj())(emptyCarrier) {}

      val result = Json.toJson[ExtendedDataEvent](event)

      val expectedTags: JsObject = Json.obj(
        "clientIP"        -> "-",
        "clientPort"      -> "-",
        "X-Request-ID"    -> "-",
        "X-Session-ID"    -> "-",
        "transactionName" -> auditType,
        "Authorization"   -> "-",
        "deviceID"        -> "-"
      )

      (result \ "tags").as[JsObject] shouldBe expectedTags
    }

    "Output with minimum tags" in {
      val event = new RegistrationAuditEvent(auditType, None, Json.obj(), TagSet.NO_TAGS)(completeCarrier) {}

      val result = Json.toJson[ExtendedDataEvent](event)

      val expectedTags: JsObject = Json.obj(
        "transactionName" -> auditType
      )

      (result \ "tags").as[JsObject] shouldBe expectedTags
    }

    "Output with name and clientIP/Port tags" in {
      val tagSet = TagSet.NO_TAGS.copy(clientIP = true, clientPort = true)
      val event = new RegistrationAuditEvent(auditType, None, Json.obj(), tagSet)(completeCarrier) {}

      val result = Json.toJson[ExtendedDataEvent](event)

      val expectedTags: JsObject = Json.obj(
        "transactionName" -> auditType,
        "clientIP" -> clientIP,
        "clientPort" -> clientPort
      )

      (result \ "tags").as[JsObject] shouldBe expectedTags
    }

    "output with name, request, session & authz tags" in {
      val tagSet = TagSet.ALL_TAGS.copy(clientIP = false, clientPort = false, deviceId = false)
      val event = new RegistrationAuditEvent(auditType, None, Json.obj(), tagSet)(completeCarrier) {}

      val result = Json.toJson[ExtendedDataEvent](event)

      val expectedTags: JsObject = Json.obj(
        "X-Request-ID" -> request,
        "X-Session-ID" -> session,
        "transactionName" -> auditType,
        "Authorization" -> bearer
      )

      (result \ "tags").as[JsObject] shouldBe expectedTags
    }


    "have the correct result" in {
      val event = new RegistrationAuditEvent(auditType, None, Json.obj())(completeCarrier) {}

      val result = Json.toJson[ExtendedDataEvent](event)

      (result \ "auditSource").as[String] shouldBe "paye-registration-frontend"
      (result \ "auditType").as[String] shouldBe auditType

      (result \ "detail").as[JsObject] shouldBe Json.obj()
    }
  }
}
