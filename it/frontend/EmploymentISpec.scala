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
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner

import java.time.LocalDate

class EmploymentISpec extends IntegrationSpecBase with LoginStub with CachingStub with BeforeAndAfterEach with WiremockHelper {

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

  def setSystemDate() = {
    stubGet(s"/paye-registration/test-only/feature-flag/system-date/2018-07-12T00:00:00Z", 200, "")
    buildClient("/test-only/feature-flag/system-date/2018-07-12T00:00:00").get()
  }

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val txId = "12345"

  val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
  val incorpUpdateSuccessResponse =
    """
      | {
      |  "crn": "fooBar",
      |  "incorporationDate": "2017-05-20"
      | }
    """.stripMargin

  val employmentInfoAlreadyEmploying =
    """
      |{
      | "employees": "alreadyEmploying",
      | "firstPaymentDate": "2017-05-21",
      | "construction": true,
      | "subcontractors": true,
      | "companyPension": true
      |}
    """.stripMargin

  val employmentInfoAlreadyEmployingNoPension =
    """
      |{
      | "employees": "alreadyEmploying",
      | "firstPaymentDate": "2017-05-21",
      | "construction": true,
      | "subcontractors": true,
      | "companyPension": false
      |}
    """.stripMargin

  val employmentInfoWillEmployThisYear =
    """
      |{
      | "employees": "willEmployThisYear",
      | "firstPaymentDate": "2018-07-12",
      | "construction": true,
      | "subcontractors": true
      |}
    """.stripMargin

  "paidEmployees" should {
    "return 200 when IncorpDate Exists and nothing exists in PR or S4L" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/employment-info", 204, "")
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      val fResponse = buildClient("/employ-anyone")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 200
    }

    "return 200 when IncorpDate Exists and block exists in PR or S4L" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/employment-info", 200, employmentInfoAlreadyEmploying)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      val fResponse = buildClient("/employ-anyone")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 200
    }

    "return 303 when no IncorpDate Exists" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/employment-info", 204, "")
      stubGet(s"/incorporation-information/$txId/incorporation-update", 204, "")

      val fResponse = buildClient("/employ-anyone")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 303
    }
  }
  "submitPaidEmployees" should {
    "return 303 if incorpDate exists" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/employment-info", 204, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/EmploymentV2", 200, dummyS4LResponse)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      val fResponse = buildClient("/employ-anyone").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "alreadyPaying" -> Seq("true"),
          "earliestDate.Day" -> Seq("21"),
          "earliestDate.Month" -> Seq("05"),
          "earliestDate.Year" -> Seq(LocalDate.now.minusYears(2).getYear.toString)
        ))
      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)

    }
    "return 303 if incorpDate does not exist" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/employment-info", 204, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/EmploymentV2", 200, dummyS4LResponse)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 204, "")

      val fResponse = buildClient("/employ-anyone").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "alreadyPaying" -> Seq("true"),
          "earliestDate.Day" -> Seq("21"),
          "earliestDate.Month" -> Seq("05"),
          "earliestDate.Year" -> Seq("2017")
        ))
      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.EmploymentController.employingStaff.url)
    }

    "return 400" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      val fResponse = buildClient("/employ-anyone").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "alreadyPaying" -> Seq("true"),
          "earliestDate.Day" -> Seq(""),
          "earliestDate.Month" -> Seq("05"),
          "earliestDate.Year" -> Seq("2017")
        ))
      val response = await(fResponse)
      response.status mustBe 400
    }
  }

  "employingStaff" should {
    "return 200 if data exists in PR" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/employment-info", 200, employmentInfoWillEmployThisYear)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      await(setSystemDate())

      val fResponse = buildClient("/will-employ-anyone")
        .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 200
    }
  }
  "submitEmployingStaff" should {
    "redirect to construction page on successful submit" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/paye-registration/$regId/employment-info", 204, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/EmploymentV2", 200, dummyS4LResponse)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      await(setSystemDate())

      val fResponse = buildClient("/will-employ-anyone").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "willBePaying" -> Seq("false")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)
    }

    "return a badrequest if nothing is answered" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/EmploymentV2", 200, dummyS4LResponse)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      await(setSystemDate())

      val fResponse = buildClient("/will-employ-anyone").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx")
        ))

      val response = await(fResponse)
      response.status mustBe 400
    }
  }

  "submitCompanyPensions" should {
    "redirect to completion capacity page on successful submit" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/EmploymentV2", 200, dummyS4LResponse)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)
      stubGet(s"/paye-registration/$regId/employment-info", 200, employmentInfoAlreadyEmploying)
      stubPatch(s"/paye-registration/$regId/employment-info", 200, employmentInfoAlreadyEmployingNoPension)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, dummyS4LResponse)

      await(setSystemDate())

      val fResponse = buildClient("/pension-payments").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "paysPension" -> Seq("false")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
    }
  }
}
