/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.{URLDecoder, URLEncoder}
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.libs.Crypto
import play.api.libs.ws.WSCookie
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Crypted, PlainText}
import uk.gov.hmrc.play.http.SessionKeys

trait LoginStub extends SessionCookieBaker {

  val SessionId = s"stubbed-${UUID.randomUUID}"

  private def cookieData(additionalData: Map[String, String], timeStampRollback: Long): Map[String, String] = {

    val timeStamp = new java.util.Date().getTime
    val rollbackTimestamp = (timeStamp - timeStampRollback).toString

    Map(
      SessionKeys.sessionId -> SessionId,
      SessionKeys.userId -> "/auth/oid/1234567890",
      SessionKeys.token -> "token",
      SessionKeys.authProvider -> "GGW",
      SessionKeys.lastRequestTimestamp -> rollbackTimestamp
    ) ++ additionalData
  }

  def getSessionCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0) = {
    cookieValue(cookieData(additionalData, timeStampRollback))
  }

  def stubSuccessfulLogin(withSignIn: Boolean = false) = {

    stubFor(get(urlEqualTo("/auth/authority"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |    "uri": "/auth/oid/1234567890",
               |    "loggedInAt": "2014-06-09T14:57:09.522Z",
               |    "previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
               |    "accounts": {
               |    },
               |    "levelOfAssurance": "2",
               |    "confidenceLevel" : 50,
               |    "credentialStrength": "strong",
               |    "legacyOid":"1234567890"
               |}
               |
            """.stripMargin
          )))
  }

  val userId = "/auth/oid/1234567890"
  def setupSimpleAuthMocks() = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"x":2}""")
      )
    )

    stubFor(get(urlMatching("/auth/authority"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s"""
                      |{
                      |"uri":"${userId}",
                      |"loggedInAt": "2014-06-09T14:57:09.522Z",
                      |"previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
                      |"credentials":{"gatewayId":"xxx2"},
                      |"accounts":{},
                      |"levelOfAssurance": "2",
                      |"confidenceLevel" : 50,
                      |"credentialStrength": "strong",
                      |"legacyOid":"1234567890",
                      |"userDetailsLink":"xxx3",
                      |"ids":"/auth/ids"
                      |}""".stripMargin)
      )
    )

    stubFor(get(urlMatching("/auth/ids"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
      )
    )
  }
}

trait SessionCookieBaker {
  val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="
  def cookieValue(sessionData: Map[String,String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(Crypto.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  def getCookieData(cookie: WSCookie): Map[String, String] = {
    getCookieData(cookie.value.get)
  }

  def getCookieData(cookieData: String): Map[String, String] = {

    val decrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).decrypt(Crypted(cookieData)).value
    val result = decrypted.split("&")
      .map(_.split("="))
      .map { case Array(k, v) => (k, URLDecoder.decode(v))}
      .toMap

    result
  }
}
