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
package www

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import itutil.{IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.test.FakeApplication
import play.api.http.HeaderNames
import uk.gov.hmrc.crypto.ApplicationCrypto


class TradingNameISpec extends IntegrationSpecBase with LoginStub with BeforeAndAfterEach with WiremockHelper {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
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
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.coho-api.host" -> s"$mockHost",
    "microservice.services.coho-api.port" -> s"$mockPort"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val userId = "/auth/oid/1234567890"
  def setupSimpleAuthMocks() = {
    stubPost("/write/audit", 200, """{"x":2}""")
    stubGet("/auth/authority", 200,
      s"""
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
         |}""".stripMargin
    )
    stubGet("/auth/ids", 200, """{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
  }

  def stubKeystore(session: String, regId: String, companyName: String) = {
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
               |   "language": "ENG"
               |  },
               |  "CoHoCompanyDetails": {
               |    "registration_id": "${regId}",
               |    "company_name": "${companyName}",
               |    "areas_of_industry": []
               |  }
               |}
               |}""".stripMargin
          )
      )
    )
  }

  "GET Trading Name" should {

    "Return a populated page if PayeReg returns an company details response" in {
      val regId = "3"
      val txId = "12345"
      val companyName = "Foo Ltd"
      val tradingName = "Foo Trading"
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubKeystore(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")

      val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
      val payeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "${tradingName}",
           |"roAddress": ${roDoc},
           |"businessContactDetails": {}
           |}""".stripMargin
      stubGet(s"/paye-registration/${regId}/company-details", 200, payeDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Trading name"
      document.getElementById("pageHeading").text should include(companyName)
      document.getElementById("differentName-true").attr("checked") shouldBe "checked"
      document.getElementById("differentName-false").attr("checked") shouldBe ""
      document.getElementById("tradingName").`val` shouldBe tradingName
    }

    "Return an unpopulated page if PayeReg returns a NotFound response" in {
      val regId = "3"
      val txId = "12345"
      val companyName = "Foo Ltd"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubKeystore(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/${regId}/company-details", 404, "")
      val crDoc = s"""{"transaction-id": "${txId}"}"""
      stubGet(s"/incorporation-frontend-stubs/corporation-tax-registration/${regId}/confirmation-references", 200, crDoc)
      val roDoc = s"""{"premises":"p", "address_line_1":"1", "locality":"l"}"""
      stubGet(s"/incorporation-frontend-stubs/${txId}/ro-address", 200, roDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Trading name"
      document.getElementById("pageHeading").text should include(companyName)
      document.getElementById("differentName-true").attr("checked") shouldBe ""
      document.getElementById("differentName-false").attr("checked") shouldBe ""
      document.getElementById("tradingName").`val` shouldBe ""

      // TODO verify S4L put
    }
  }

//  "POST Trading Name" should {
//
//    implicit val jsonCrypto = ApplicationCrypto.JsonCrypto
//
//    "Accept information and send to PR" in {
//      val regId = "3"
//      val txId = "12345"
//      val companyName = "Foo Ltd"
//      val tradingName = "Foo Trading"
//
//      setupSimpleAuthMocks()
//
//      val csrfToken = UUID.randomUUID().toString
//
//      stubKeystore(SessionId, regId, companyName)
//
////      val crResponse = """{"accountingDateStatus":"WHEN_REGISTERED", "links": []}"""
////      stubPut("/company-registration/corporation-tax-registration/5/accounting-details", 200, crResponse)
//
//      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
//
//      val fResponse = buildClient("/trading-name").
//        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
//        post(Map(
//          "csrfToken"->Seq("xxx-ignored-xxx"),
//          "businessStartDate"->Seq("futureDate"),
//          "businessStartDate.year"->Seq("2018"),
//          "businessStartDate.month"->Seq("1"),
//          "businessStartDate.day"->Seq("2")
//        ))
//
//      val response = await(fResponse)
//
//      response.status shouldBe 303
//      response.header(HeaderNames.LOCATION) shouldBe Some("/register-your-company/trading-details")
//
//      val crPuts = findAll(putRequestedFor(urlMatching("/company-registration/corporation-tax-registration/5/accounting-details")))
//      val captor = crPuts.get(0)
//      val json = Json.parse(captor.getBodyAsString)
//      (json \ "accountingDateStatus").as[String] shouldBe "FUTURE_DATE"
//      (json \ "startDateOfBusiness").as[String] shouldBe "2018-01-02"
//    }
//  }

}