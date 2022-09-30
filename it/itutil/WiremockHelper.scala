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

package itutil

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSRequest}

object WiremockHelper {
  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val url = s"http://$wiremockHost:$wiremockPort"
}

trait WiremockHelper {
  self: GuiceOneServerPerSuite =>

  import WiremockHelper._

  lazy val wmConfig = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

  def buildClient(path: String): WSRequest =
    ws
      .url(s"http://localhost:$port/register-for-paye$path")
      .withFollowRedirects(false)

  def buildClientInternal(path: String, additionalHeaders: (String, String)*): WSRequest =
    ws
      .url(s"http://localhost:$port/internal$path")
      .withHttpHeaders(Seq(HeaderNames.AUTHORIZATION -> "FooBarToken") ++ additionalHeaders:_*)
      .withFollowRedirects(false)


  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String, responseHeader: (String, String)): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
          withHeader(responseHeader._1, responseHeader._2)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPut(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPatch(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(patch(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubDelete(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(delete(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
}
