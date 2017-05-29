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

package itutil

import com.github.tomakehurst.wiremock.client.WireMock._
import enums.PAYEStatus
import models.Address
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.crypto.json.JsonEncryptor
import uk.gov.hmrc.crypto.{ApplicationCrypto, Protected}

import scala.concurrent.Future

trait CachingStub {

  implicit lazy val jsonCrypto = ApplicationCrypto.JsonCrypto
  implicit lazy val encryptionFormat = new JsonEncryptor[JsObject]()

  def stubKeystoreMetadata(session: String, regId: String, companyName: String) = {
    val keystoreUrl = s"/keystore/paye-registration-frontend/${session}"
    stubFor(get(urlMatching(keystoreUrl))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
               |"id": "${session}",
               |"data": {
               | "CurrentProfile": {
               |   "registrationID": "${regId}",
               |   "completionCapacity": "Director",
               |   "companyTaxRegistration": {
               |      "status": "submitted",
               |      "transactionId": "12345"
               |   },
               |   "language": "ENG"
               |  },
               |  "CoHoCompanyDetails": {
               |    "company_name": "${companyName}",
               |    "registered_office_address": {
               |      "line1":"Line1",
               |      "line2":"Line2",
               |      "postCode":"TE1 1ST"
               |    }
               |  }
               |}
               |}""".stripMargin
          )
      )
    )
  }

  def stubEmptyKeystore(sessionId: String) = {
    val keystoreUrl = s"/keystore/paye-registration-frontend/$sessionId"
    stubFor(get(urlMatching(keystoreUrl))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
               |"id": "$sessionId",
               |"data": {}
               |}""".stripMargin
          )
      )
    )
  }

  def stubKeystoreDelete(sessionId: String) = {
    stubFor(delete(urlMatching(s"/keystore/paye-registration-frontend/$sessionId"))
      .willReturn(
        aResponse()
          .withStatus(200)
      )
    )
  }

  def stubPayeRegDocumentStatus(regId: String) = {
    val payeRegUrl = s"/paye-registration/$regId/status"
    stubFor(get(urlMatching(payeRegUrl))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "status" : "draft"
               |}
             """.stripMargin
          )
      )
    )
  }

  def stubS4LGet(regId: String, key: String, data: String) = {
    val s4lData = Json.parse(data).as[JsObject]
    val encData = encryptionFormat.writes(Protected(s4lData)).as[JsString]

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(get(urlMatching(s"/save4later/paye-registration-frontend/$regId"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }

  def stubS4LPut(regId: String, key: String, data: String) = {
    val s4lData = Json.parse(data).as[JsObject]
    val encData = encryptionFormat.writes(Protected(s4lData)).as[JsString]

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(put(urlMatching(s"/save4later/paye-registration-frontend/$regId/data/$key"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }

}
