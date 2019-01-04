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

package internal

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import models.api.SessionMap
import models.external.{BusinessProfile, CompanyRegistrationProfile, CurrentProfile}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.FakeApplication

class RegistrationMethodISpec extends IntegrationSpecBase
                          with LoginStub
                          with CachingStub
                          with BeforeAndAfterEach
                          with WiremockHelper {
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
    "microservice.services.cachable.short-lived-cache.host" -> s"$mockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$mockPort",
    "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-frontend-stubs.host" -> s"$mockHost",
    "microservice.services.incorporation-frontend-stubs.port" -> s"$mockPort",
    "Csrf-Bypass-value" -> "bm9jaGVjaw==",
    "mongodb.uri" -> s"$mongoUri"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val companyName = "Foo Ltd"
  val authorisationToken = "FooBarWizzAuthToken"

  "DELETE registration" should {
    "return 401 if user is not authorised for the session" in {
      setupUnauthorised()

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> "1112223355556","Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 401
    }

    "return 400 if the Registration ID requested for delete is not same as the CurrentProfile from Keystore" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata("sessionId", regId)

      val fResponse = buildClientInternal(s"/4/delete").
        withHeaders("X-Session-ID" -> "sessionId", "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 400
    }

    "return 400 if the Registration ID requested for delete is not same as the CurrentProfile" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId,regId,false)

      val businessProfile = BusinessProfile(
        regId,
        "ENG"
      )

      val companyRegistrationResp =
        s"""{
           |   "status": "draft",
           |   "confirmationReferences": {
           |     "transaction-id": "000-434-$regId"
           |   }
           |}""".stripMargin

      stubGet(s"/paye-registration/$regId/status", 200, """{"status": "draft"}""")
      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
      stubGet(s"/incorporation-frontend-stubs/$regId/corporation-tax-registration", 200, companyRegistrationResp)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPost(s"/incorporation-information/subscribe/000-434-$regId/regime/paye-fe/subscriber/SCRS", 202, "")

      val fResponse = buildClientInternal(s"/4/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)
      response.status mustBe 400
    }

    "return 412 if the Registration is not in Draft or Rejected status for deletion" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 204, "")

      stubDelete(s"/paye-registration/$regId/delete-in-progress", 412, "")

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)
      response.status mustBe 412
    }

    "return 500 if the Registration is not found" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 204, "")

      stubDelete(s"/paye-registration/$regId/delete-in-progress", 404, "")

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 500
    }

    "return 500 if PAYERegistration microservice returns 403" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 204, "")

      stubDelete(s"/paye-registration/$regId/delete-in-progress", 403, "")

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 500
    }

    "return 500 if PAYERegistration microservice fail to delete Registration" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 204, "")

      stubDelete(s"/paye-registration/$regId/delete-in-progress", 500, "")

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 500
    }

    "return 500 if S4L fail to clear Registration" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 500, "")

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 500
    }

    "return 200 if PAYERegistration microservice successfully delete Registration" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regId)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 204, "")

      stubDelete(s"/paye-registration/$regId/delete-in-progress", 200, "")

      val fResponse = buildClientInternal(s"/$regId/delete").
        withHeaders("X-Session-ID" -> SessionId, "Authorization" -> authorisationToken).
        delete()

      val response = await(fResponse)

      response.status mustBe 200
    }
  }

  "companyIncorporation" should {
    val testIIBody = Json.parse(
      """
        |{
        |   "SCRSIncorpStatus" : {
        |       "IncorpSubscriptionKey" : {
        |           "transactionId" : "testTxId"
        |       },
        |       "IncorpStatusEvent" : {
        |           "status" : "rejected"
        |       }
        |   }
        |}
      """.stripMargin
    )

    val testSessionMap = SessionMap(
      sessionId      = "testSessionId",
      registrationId = s"$regId",
      transactionId  = "testTxId",
      data           = Map(
        "CurrentProfile" -> Json.toJson(CurrentProfile(
          registrationID            = s"$regId",
          companyTaxRegistration    = CompanyRegistrationProfile(
            status        = "",
            transactionId = "testTxId"
          ),
          language                  = "en",
          payeRegistrationSubmitted = false,
          incorpStatus              = None
        ))
      )
    )

    "return an Ok" when {
      "the users incorp status has been updated and they have a active session" in {
        await(repo.upsertSessionMap(testSessionMap))

        stubDelete(s"/save4later/paye-registration-frontend/$regId", NO_CONTENT, "")
        stubDelete(s"/paye-registration/$regId/delete-rejected-incorp", OK, "")

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe OK
      }

      "the user incorp status has been updated but the user is not present" in {
        stubGet("/paye-registration/testTxId/registration-id", OK, "testRegId")

        stubDelete(s"/save4later/paye-registration-frontend/$regId", NO_CONTENT, "")
        stubDelete(s"/paye-registration/$regId/delete-rejected-incorp", OK, "")

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe OK
      }
      "getRegIdReturns 404" in {
        stubGet("/paye-registration/testTxId/registration-id", NOT_FOUND, "testRegId")

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe OK
      }

      "delete reject incorp returns 404" in {
        stubGet("/paye-registration/testTxId/registration-id", OK, "testRegId")

        stubDelete(s"/save4later/paye-registration-frontend/$regId", NO_CONTENT, "")
        stubDelete(s"/paye-registration/$regId/delete-rejected-incorp", NOT_FOUND, "")

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
      }

      "the user incorp status but not deleted because the status wasn't rejected and there is an active session" in {
        val testIIBody = Json.parse(
          """
            |{
            |   "SCRSIncorpStatus" : {
            |       "IncorpSubscriptionKey" : {
            |           "transactionId" : "testTxId"
            |       },
            |       "IncorpStatusEvent" : {
            |           "status" : "accepted"
            |       }
            |   }
            |}
          """.stripMargin
        )

        await(repo.upsertSessionMap(testSessionMap))

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe OK
      }

      "the user incorp status but not deleted because the status wasn't rejected but there is no user present" in {
        val testIIBody = Json.parse(
          """
            |{
            |   "SCRSIncorpStatus" : {
            |       "IncorpSubscriptionKey" : {
            |           "transactionId" : "testTxId"
            |       },
            |       "IncorpStatusEvent" : {
            |           "status" : "accepted"
            |       }
            |   }
            |}
          """.stripMargin
        )

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe OK
      }
    }

    "return an InternalServerError" when {
      "paye reg returns 500 when deleting rejected incorp and they have an active session" in {
        await(repo.upsertSessionMap(testSessionMap))

        stubDelete(s"/save4later/paye-registration-frontend/$regId", INTERNAL_SERVER_ERROR, "")
        stubDelete(s"/paye-registration/$regId/delete-rejected-incorp", INTERNAL_SERVER_ERROR, "")

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "paye reg returns 500 when deleting rejected incorp but the user is not present" in {
        stubGet("/paye-registration/testTxId/registration-id", NOT_FOUND, "")

        stubDelete(s"/save4later/paye-registration-frontend/$regId", INTERNAL_SERVER_ERROR, "")
        stubDelete(s"/paye-registration/$regId/delete-rejected-incorp", INTERNAL_SERVER_ERROR, "")

        val result = await(buildClientInternal("/company-incorporation").post(testIIBody))
        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "there was a problem parsing out the required data from the II body" in {

        val result = await(buildClientInternal("/company-incorporation").post(Json.parse("""{ "abc" : "xyz"}""")))
        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
