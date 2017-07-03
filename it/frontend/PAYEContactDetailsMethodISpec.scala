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
package frontend

import java.util.UUID

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.FakeApplication


class PAYEContactDetailsMethodISpec extends IntegrationSpecBase
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
    "microservice.services.business-registration.port" -> s"$mockPort"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val companyName = "Test Company Ltd"

  "GET PAYE Contact details" should {

    "not be prepoulated if no data is found in Paye Registration and error is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 403, "")
      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val response = await(buildClient("/who-should-we-contact")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Who should we contact about the company's PAYE?"
      document.getElementById("name").data() shouldBe ""
      document.getElementById("digitalContact.contactEmail").attr("value") shouldBe ""
      document.getElementById("digitalContact.mobileNumber").attr("value") shouldBe ""
      document.getElementById("digitalContact.phoneNumber").attr("value") shouldBe ""
    }

    "Return an unpopulated page if PayeReg returns a NotFound response" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      val validPrepopResponse =
        s"""
           |{
           |   "firstName": "fName",
           |   "middleName": "mName1 mName2",
           |   "surname": "sName",
           |   "email": "email1",
           |   "telephoneNumber": "012345",
           |   "mobileNumber": "543210"
           |}
         """.stripMargin

      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, validPrepopResponse)
      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val response = await(buildClient("/who-should-we-contact")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Who should we contact about the company's PAYE?"
      document.getElementById("name").attr("value") shouldBe "fName mName1 mName2 sName"
      document.getElementById("digitalContact.contactEmail").attr("value") shouldBe "email1"
      document.getElementById("digitalContact.mobileNumber").attr("value") shouldBe "543210"
      document.getElementById("digitalContact.phoneNumber").attr("value") shouldBe "012345"
    }
  }

  "POST PAYE Contact details" should {
    val csrfToken = UUID.randomUUID().toString

    "upsert the contact details in Business Registration" in {
      val first = "Simon"
      val middle = "Test"
      val last = "Name"
      val newName = s"$first $middle $last"
      val newEmail = "newEmail@email.biz.co.uk"
      val newTelephoneNumber = "02123456789"
      val newMobileNumber = "07123456789"

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      val updatedPayeDoc =
        s"""{
           |   "correspondenceAddress": {"line1":"1","line2":"2","postCode":"pc"},
           |   "contactDetails": {
           |      "name": "$newName",
           |      "digitalContactDetails": {
           |        "email": "$newEmail",
           |        "phoneNumber": "$newMobileNumber",
           |        "mobileNumber": "$newTelephoneNumber"
           |      }
           |   }
           |}""".stripMargin
      stubPatch(s"/paye-registration/${regId}/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/${regId}/contact-correspond-paye", 200, updatedPayeDoc)

      val updatedContactDetail =
        s"""
           |{
           |   "name": "$newName",
           |   "email": "$newEmail",
           |   "telephoneNumber": "$newTelephoneNumber",
           |   "mobileNumber": "$newMobileNumber"
           |}
       """.stripMargin
      stubPost(s"/business-registration/${regId}/contact-details", 200, updatedContactDetail)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      stubGet(s"/paye-registration/$regId/company-details", 404, "")

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)
      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/who-should-we-contact").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "name" -> Seq(s"$newName"),
          "digitalContact.contactEmail" -> Seq(s"$newEmail"),
          "digitalContact.phoneNumber" -> Seq(s"$newTelephoneNumber"),
          "digitalContact.mobileNumber" -> Seq(s"$newMobileNumber")
        ))

      val response = await(fResponse)
      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/where-to-send-post")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/${regId}/contact-details")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)


      val prepopJson =
        s"""
           |{
           |   "firstName": "$first",
           |   "middleName": "$middle",
           |   "surname": "$last",
           |   "email": "$newEmail",
           |   "telephoneNumber": "$newTelephoneNumber",
           |   "mobileNumber": "$newMobileNumber"
           |}
       """.stripMargin

      json shouldBe Json.parse(prepopJson)
    }
  }
}