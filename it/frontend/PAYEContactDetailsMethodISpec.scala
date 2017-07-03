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
import enums.CacheKeys
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
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val companyName = "Test Company Ltd"

  "GET PAYE Contact details" should {

    "not be prepopulated if no data is found in Paye Registration and error is returned from Business Registration" in {
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
           |        "phoneNumber": "$newTelephoneNumber",
           |        "mobileNumber": "$newMobileNumber"
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

  "GET Correspondence Address" should {
    val tradingName = "Foo Trading"
    val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
    val payeDoc =
      s"""{
         |"companyName": "${companyName}",
         |"tradingName": "${tradingName}",
         |"roAddress": ${roDoc},
         |"ppobAddress": ${roDoc},
         |"businessContactDetails": {}
         |}""".stripMargin

    "not be prepopulated if an error is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/business-registration/$regId/addresses", 403, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val response = await(buildClient("/where-to-send-post")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where should we send post to?"
      document.getElementById("chosenAddress-roaddress").attr("value") shouldBe "roAddress"
      document.getElementById("ro-address-line-1").text shouldBe "1"
      document.getElementById("ro-address-line-2").text shouldBe ", 2"
      document.getElementById("ro-post-code").text shouldBe ", pc"

      an[Exception] shouldBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))

      document.getElementById("chosenAddress-other").attr("value") shouldBe "other"
    }

    "not be prepopulated if a wrong address is returned from Business Registration" in {
      val addresses =
        s"""{
           |  "addresses": [
           |    {
           |      "addressLine1": "prepopLine1",
           |      "addressLine2": "prepopLine2",
           |      "postcode": "wrongPostcode"
           |    },
           |    {
           |      "addressLine1": "prepopLine11",
           |      "addressLine2": "prepopLine22",
           |      "addressLine3": "prepopLine33",
           |      "country": "prepopCountry"
           |    }
           |  ]
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val response = await(buildClient("/where-to-send-post")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      an[Exception] shouldBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))
    }

    "be prepopulated if data is returned from Business Registration" in {
      val addresses =
        s"""{
           |  "addresses": [
           |    {
           |      "addressLine1": "prepopLine1",
           |      "addressLine2": "prepopLine2",
           |      "postcode": "AB9 8ZZ"
           |    },
           |    {
           |      "addressLine1": "prepopLine11",
           |      "addressLine2": "prepopLine22",
           |      "addressLine3": "prepopLine33",
           |      "country": "prepopCountry"
           |    }
           |  ]
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val response = await(buildClient("/where-to-send-post")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where should we send post to?"
      document.getElementById("chosenAddress-roaddress").attr("value") shouldBe "roAddress"
      document.getElementById("chosenAddress-roaddress").attr("name") shouldBe "chosenAddress"
      document.getElementById("ro-address-line-1").text shouldBe "1"
      document.getElementById("ro-address-line-2").text shouldBe ", 2"
      document.getElementById("ro-post-code").text shouldBe ", pc"

      document.getElementById("chosenAddress-prepopaddress0").attr("value") shouldBe "prepopAddress0"
      document.getElementById("chosenAddress-prepopaddress0").attr("name") shouldBe "chosenAddress"
      document.getElementById("prepopaddress0-address-line-1").text shouldBe "prepopLine1"
      document.getElementById("prepopaddress0-address-line-2").text shouldBe ", prepopLine2"
      document.getElementById("prepopaddress0-post-code").text shouldBe ", AB9 8ZZ"

      document.getElementById("chosenAddress-prepopaddress1").attr("value") shouldBe "prepopAddress1"
      document.getElementById("chosenAddress-prepopaddress1").attr("name") shouldBe "chosenAddress"
      document.getElementById("prepopaddress1-address-line-1").text shouldBe "prepopLine11"
      document.getElementById("prepopaddress1-address-line-2").text shouldBe ", prepopLine22"
      document.getElementById("prepopaddress1-address-line-3").text shouldBe ", prepopLine33"
      document.getElementById("prepopaddress1-country").text shouldBe ", prepopCountry"

      document.getElementById("chosenAddress-other").attr("value") shouldBe "other"
      document.getElementById("chosenAddress-other").attr("name") shouldBe "chosenAddress"
    }
  }

  "POST Correspondence Address" should {
    val csrfToken = UUID.randomUUID().toString
    val addresses =
      s"""{
         |  "13": {
         |    "line1": "prepopLine1",
         |    "line2": "prepopLine2",
         |    "postCode": "prepopPC0"
         |  },
         |  "1": {
         |    "line1": "prepopLine11",
         |    "line2": "prepopLine22",
         |    "line3": "prepopLine33",
         |    "postCode": "prepopPC1"
         |  }
         |}""".stripMargin
    val updatedPayeDoc =
      s"""{
         |   "correspondenceAddress": {"line1":"prepopLine1","line2":"prepopLine2","postCode":"prepopPC0"},
         |   "contactDetails": {
         |      "name": "PAYEContactName",
         |      "digitalContactDetails": {
         |        "email": "test@email.uk",
         |        "phoneNumber": "021234",
         |        "mobileNumber": "071234"
         |      }
         |   }
         |}""".stripMargin

    "upsert PAYE Contact Details in PAYE Registration with a prepop address" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-to-send-post").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/check-and-confirm-your-answers")

      val reqPosts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/${regId}/contact-correspond-paye")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json shouldBe Json.parse(updatedPayeDoc)
    }
  }

  "GET savePAYECorrespondenceAddress" should {
    val addressLookupID = "888"
    val updatedPayeDoc =
      s"""{
         |   "correspondenceAddress": {"line1":"prepopLine1","line2":"prepopLine2","postCode":"prepopPC0"},
         |   "contactDetails": {
         |      "name": "PAYEContactName",
         |      "digitalContactDetails": {
         |        "email": "test@email.uk",
         |        "phoneNumber": "021234",
         |        "mobileNumber": "071234"
         |      }
         |   }
         |}""".stripMargin

    "upsert PAYE Contact Details in PAYE Registration and upsert addresses in Business Registration with an address from Address Lookup" in {
      val addressAuditRef = "tstAuditRef"
      val addressLine1 = "14 St Test Walker"
      val addressLine2 = "Testford"
      val addressLine3 = "Testley"
      val addressLine4 = "Testshire"
      val addressPostcode = "TE1 1ST"
      val addressFromALF = s"""{
                             |  "auditRef":"$addressAuditRef",
                             |  "address":{
                             |    "lines":[
                             |      "$addressLine1",
                             |      "$addressLine2",
                             |      "$addressLine3",
                             |      "$addressLine4"
                             |    ],
                             |    "postcode":"$addressPostcode",
                             |    "country":{
                             |      "code":"UK",
                             |      "name":"United Kingdom"
                             |    }
                             |  }
                             |}""".stripMargin

      val newAddress2BusReg =
        s"""
           |{
           |   "auditRef": "$addressAuditRef",
           |   "addressLine1": "$addressLine1",
           |   "addressLine2": "$addressLine2",
           |   "addressLine3": "$addressLine3",
           |   "addressLine4": "$addressLine4",
           |   "postcode": "$addressPostcode"
           |}
       """.stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubPost(s"/business-registration/${regId}/addresses", 200, newAddress2BusReg)
      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")
      stubGet(s"/api/confirmed?id=$addressLookupID", 200, addressFromALF)

      val response = await(buildClient(s"/return-from-address-for-corresp-addr?id=$addressLookupID")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/check-and-confirm-your-answers")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/${regId}/addresses")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json shouldBe Json.parse(newAddress2BusReg)
    }
  }
}