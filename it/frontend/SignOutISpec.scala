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
package frontend

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import play.api.test.FakeApplication

class SignOutISpec extends IntegrationSpecBase with LoginStub with CachingStub with BeforeAndAfterEach with WiremockHelper {


  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.company-registration-frontend.www.url" -> s"$mockUrl",
    "microservice.services.company-registration-frontend.www.uri" -> "/register-your-company",
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  private def getMDTPSessionCookie(resp: WSResponse) = getCookieData(resp.cookie("mdtp").get).get("sessionId")

  "Signing out from the welcome page" should {
    "redirect the user to the exit questionnaire and clear the session" when {
      "hitting /register-for-paye/sign-out" in {
        setupSimpleAuthMocks()
        stubSuccessfulLogin()
        stubEmptyKeystore(SessionId)
        stubKeystoreDelete(SessionId)

        await(buildClient("/").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get())

        val signOutResponse = await(buildClient("/sign-out").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get())
        signOutResponse.header("location") shouldBe Some(s"$mockUrl/register-your-company/questionnaire")
        getMDTPSessionCookie(signOutResponse) shouldBe None
      }
    }
  }
}
