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

package frontend

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.DefaultWSCookie

class LanguageControllerISpec extends IntegrationSpecBase with LoginStub with CachingStub with BeforeAndAfterEach with WiremockHelper {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val config = Map(
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.cachable.short-lived-cache.host" -> s"$mockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$mockPort",
    "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.company-registration-frontend.www.url" -> s"$mockHost",
    "microservice.services.company-registration-frontend.www.uri" -> "/test-uri",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "mongodb.uri" -> s"$mongoUri"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  lazy val defaultCookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  override def beforeEach() {
    resetWiremock()
  }

  "setLanguage" should {
    val welshCookie = Some(DefaultWSCookie("PLAY_LANG", "cy", None, Some("/"), None, true, false))
    val englishCookie = Some(DefaultWSCookie("PLAY_LANG", "en", None, Some("/"), None, true, false))

    "return a 303 when language is switched" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()

      val fResponse = buildClient("/defaultLanguage/english")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 303
    }

    "return Welsh cookie when language is switched to Welsh" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()

      val fResponse = buildClient("/defaultLanguage/cymraeg")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.cookie("PLAY_LANG") mustBe welshCookie
    }

    "return English cookie when language is switched to English" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()

      val fResponse = buildClient("/defaultLanguage/english")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.cookie("PLAY_LANG") mustBe englishCookie
    }
  }
}
