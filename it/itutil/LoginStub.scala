/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSCookie
import uk.gov.hmrc.crypto.{Crypted, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.http.SessionKeys

import java.net.{URLDecoder, URLEncoder}
import java.util.UUID

trait LoginStub extends SessionCookieBaker {

  val SessionId = s"stubbed-${UUID.randomUUID}"
  val invalidSessionId = s"FAKE_PRF::NON-COMPSDOJ OMSDDf"

  private def cookieData(additionalData: Map[String, String], timeStampRollback: Long, sessionID: String): Map[String, String] = {

    val timeStamp = new java.util.Date().getTime
    val rollbackTimestamp = (timeStamp - timeStampRollback).toString

    Map(
      SessionKeys.sessionId -> sessionID,
      SessionKeys.lastRequestTimestamp -> rollbackTimestamp,
      SessionKeys.authToken -> "FooBarToken"
    ) ++ additionalData
  }

  def getSessionCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0, sessionID: String = SessionId) = {
    cookieValue(cookieData(additionalData, timeStampRollback, sessionID))
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
               |    "userDetailsLink": "/user-details/id/1234567890",
               |    "ids": "/auth/oid/1234567890/ids",
               |    "legacyOid":"1234567890"
               |}
               |
            """.stripMargin
          )))

    stubFor(get(urlMatching("/user-details/id/1234567890"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
               |  "name": "testUserName",
               |  "email": "testUserEmail",
               |  "affinityGroup": "testAffinityGroup",
               |  "authProviderId": "testAuthProviderId",
               |  "authProviderType": "testAuthProviderType"
               |}""".stripMargin)
      )
    )

    stubFor(get(urlMatching("/auth/oid/1234567890/ids"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
      )
    )
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

    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody("""{"internalId": "Int-xxx"}""")
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

  def setupUnauthorised() = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"x":2}""")
      )
    )

    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(401)
      )
    )
  }

  def setupAuthMocks() = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"x":2}""")
      )
    )

    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            """
              |{
              | "externalId": "Ext-xxx",
              | "optionalCredentials" : {
              |   "providerId" : "testAuthProviderId",
              |   "providerType" : "GG"
              | },
              | "name": {
              |       "name": "testFirstName",
              |       "lastName": "testLastName"
              |    }
              |}
            """.stripMargin)
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
  val defaultCookieSigner: DefaultCookieSigner
  val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="

  def cookieValue(sessionData: Map[String, String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(defaultCookieSigner.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = SymmetricCryptoFactory.aesGcmCrypto(cookieKey).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  def getCookieData(cookie: WSCookie): Map[String, String] = {
    getCookieData(cookie.value)
  }

  def getCookieData(cookieData: String): Map[String, String] = {

    val decrypted = SymmetricCryptoFactory.aesGcmCrypto(cookieKey).decrypt(Crypted(cookieData)).value
    val result = decrypted.split("&")
      .map(_.split("="))
      .map { case Array(k, v) => (k, URLDecoder.decode(v, "UTF-8")) }
      .toMap

    result
  }
}
