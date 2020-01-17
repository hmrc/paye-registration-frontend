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

package frontend

import controllers.userJourney.routes
import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.FakeApplication

class PageVisibilityISpec extends IntegrationSpecBase
with LoginStub
with CachingStub
with BeforeAndAfterEach
with WiremockHelper {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.cachable.short-lived-cache.host" -> s"$mockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$mockPort",
    "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
    "mongodb.uri" -> s"$mongoUri"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "98765"

  def stubCompanyDetailsBackendFetch() = {
    val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"TE1 1ST"}"""
    val payeDoc =
      s"""{
         |"companyName": "TstCompanyName",
         |"roAddress": $roDoc,
         |"ppobAddress": $roDoc,
         |"businessContactDetails": {}
         |}""".stripMargin
    stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
  }

  def stubEmptyNatureOfBusiness() = stubGet(s"/paye-registration/$regId/sic-codes", 200, "[]")

  def currentProfileJsonString(regSubmitted: Option[Boolean], regId: String = "12345") = Json.parse(
    s"""{
       | "CurrentProfile": {
       |    "registrationID":"$regId",
       |    "completionCapacity":"Director",
       |    "companyTaxRegistration":{
       |      "status":"acknowledged",
       |      "transactionId":"40-654321"
       |    },
       |    "language":"ENG"${regSubmitted.map(bool => s""", "payeRegistrationSubmitted":$bool """).getOrElse("")}
       |  }
       |}
     """.stripMargin).toString()

  "GET Nature of business" should {

    "Show the page when paye registration has not been submitted" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

//      stubKeystoreGet(SessionId, currentProfileJsonString(regSubmitted = Some(false), regId = regId))
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubEmptyNatureOfBusiness()

      stubCompanyDetailsBackendFetch()

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/what-company-does").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(sessionID = SessionId)).
        get()

      val response = await(fResponse)

      response.status mustBe 200
    }

    "Show the page when payeRegistrationSubmitted is not present in Keystore" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()


      stubSessionCacheMetadata(SessionId, regId)
//      stubKeystoreGet(SessionId, currentProfileJsonString(regSubmitted = None, regId = regId))

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubEmptyNatureOfBusiness()

      stubCompanyDetailsBackendFetch()

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/what-company-does").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
    }

    "Redirect to the dashboard when paye registration has been submitted" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId, true)
//      stubKeystoreGet(SessionId, currentProfileJsonString(regSubmitted = Some(true), regId = regId))

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubEmptyNatureOfBusiness()

      stubCompanyDetailsBackendFetch()

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/what-company-does").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 303
      response.header("Location") mustBe Some(routes.DashboardController.dashboard().url)
    }
  }

  "GET Acknowledgement screen" should {

    "Show the page when paye registration has been submitted" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId, true)
//      stubKeystoreGet(SessionId, currentProfileJsonString(regSubmitted = Some(true), regId = regId))

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubGet(s"/paye-registration/$regId/acknowledgement-reference", 200, "\"ackRef\"")

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

      val fResponse = buildClient("/application-submitted").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
    }
  }

}
