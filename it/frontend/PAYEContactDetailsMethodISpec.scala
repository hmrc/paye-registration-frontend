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

import java.util.UUID

import audit.AuditPAYEContactDetails
import com.github.tomakehurst.wiremock.client.WireMock._
import enums.CacheKeys
import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, JsString, Json}
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
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "mongodb.uri" -> s"$mongoUri"
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
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 403, "")
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

      val response = await(buildClient("/who-should-we-contact")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Who should we contact about the company's PAYE?"
      document.getElementById("name").data() mustBe ""
      document.getElementById("digitalContact.contactEmail").attr("value") mustBe ""
      document.getElementById("digitalContact.mobileNumber").attr("value") mustBe ""
      document.getElementById("digitalContact.phoneNumber").attr("value") mustBe ""
    }

    "Return an unpopulated page if PayeReg returns a NotFound response and corrupted data is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val invalidPrepopResponse =
        s"""
           |{
           |   "email": "email1",
           |   "telephoneNumber": "012345",
           |   "mobileNumber": "543210"
           |}
         """.stripMargin

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, invalidPrepopResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrepopPAYEContactDetails", 200, dummyS4LResponse)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

      val response = await(buildClient("/who-should-we-contact")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Who should we contact about the company's PAYE?"
      document.getElementById("name").attr("value") mustBe ""
      document.getElementById("digitalContact.contactEmail").attr("value") mustBe ""
      document.getElementById("digitalContact.mobileNumber").attr("value") mustBe ""
      document.getElementById("digitalContact.phoneNumber").attr("value") mustBe ""
    }

    "Return a prepopulated page if PayeReg returns a NotFound response and data is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val validPrepopResponse =
        s"""
           |{
           |   "firstName": "fName",
           |   "middleName": "mName1 mName2",
           |   "surname": "sName",
           |   "email": "email1@email.co.uk",
           |   "telephoneNumber": "012345012345",
           |   "mobileNumber": "543210543210"
           |}
         """.stripMargin

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, validPrepopResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrepopPAYEContactDetails", 200, dummyS4LResponse)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

      val response = await(buildClient("/who-should-we-contact")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Who should we contact about the company's PAYE?"
      document.getElementById("name").attr("value") mustBe "fName mName1 mName2 sName"
      document.getElementById("digitalContact.contactEmail").attr("value") mustBe "email1@email.co.uk"
      document.getElementById("digitalContact.mobileNumber").attr("value") mustBe "543210543210"
      document.getElementById("digitalContact.phoneNumber").attr("value") mustBe "012345012345"
    }
    "Return a prepopulated page only with Digital contact details as name is no longer mandatory from prepop" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val validPrepopResponse =
        s"""
           |{
           |   "email": "email1@email.co.uk",
           |   "telephoneNumber": "012345012345",
           |   "mobileNumber": "543210543210"
           |}
         """.stripMargin

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, validPrepopResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrepopPAYEContactDetails", 200, dummyS4LResponse)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

      val response = await(buildClient("/who-should-we-contact")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Who should we contact about the company's PAYE?"
      document.getElementById("name").attr("value") mustBe ""
      document.getElementById("digitalContact.contactEmail").attr("value") mustBe "email1@email.co.uk"
      document.getElementById("digitalContact.mobileNumber").attr("value") mustBe "543210543210"
      document.getElementById("digitalContact.phoneNumber").attr("value") mustBe "012345012345"
    }
  }

  "POST PAYE Contact details" should {
    val csrfToken = UUID.randomUUID().toString
    val first = "Simon"
    val middle = "Test"
    val last = "Name"
    val oldName = "OLD OLD NAME"
    val oldEmail = "oldEmail@email.co.uk"
    val newName = s"$first $middle $last"
    val newEmail = "newEmail@email.biz.co.uk"
    val newTelephoneNumber = "02123456789"
    val newMobileNumber = "07123456789"

    "upsert the contact details in Business Registration" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val currentPayeDoc =
        s"""{
           |   "correspondenceAddress": {"line1":"1","line2":"2","postCode":"pc"},
           |   "contactDetails": {
           |      "name": "$oldName",
           |      "digitalContactDetails": {
           |        "email": "$oldEmail"
           |      }
           |   }
           |}""".stripMargin

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

      stubS4LGet(regId, CacheKeys.PAYEContact.toString, currentPayeDoc)
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 200, currentPayeDoc)
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)

      val updatedContactDetail =
        s"""
           |{
           |   "name": "$newName",
           |   "email": "$newEmail",
           |   "telephoneNumber": "$newTelephoneNumber",
           |   "mobileNumber": "$newMobileNumber"
           |}
       """.stripMargin
      stubPost(s"/business-registration/$regId/contact-details", 200, updatedContactDetail)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      stubGet(s"/paye-registration/$regId/company-details", 404, "")

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

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
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/where-to-send-post")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/$regId/contact-details")))
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

      json mustBe Json.parse(prepopJson)

      val reqPostsAudit = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPostsAudit.get(0)
      val jsonAudit = Json.parse(captorPost.getBodyAsString)

      val previousPAYEContactDetails = AuditPAYEContactDetails(
        oldName,
        Some(oldEmail),
        None,
        None
      )

      val newPAYEContactDetails = AuditPAYEContactDetails(
        newName,
        Some(newEmail),
        Some(newMobileNumber),
        Some(newTelephoneNumber)
      )

      (jsonAudit \ "auditSource").as[JsString].value mustBe "paye-registration-frontend"
      (jsonAudit \ "auditType").as[JsString].value mustBe "payeContactDetailsAmendment"
      (jsonAudit \ "detail" \ "externalUserId").as[JsString].value mustBe "Ext-xxx"
      (jsonAudit \ "detail" \ "authProviderId").as[JsString].value mustBe "testAuthProviderId"
      (jsonAudit \ "detail" \ "journeyId").as[JsString].value mustBe regId
      (jsonAudit \ "detail" \ "previousPAYEContactDetails").as[AuditPAYEContactDetails] mustBe previousPAYEContactDetails
      (jsonAudit \ "detail" \ "newPAYEContactDetails").as[AuditPAYEContactDetails] mustBe newPAYEContactDetails

      val tags = (jsonAudit \ "tags").as[JsObject].value
      tags("clientIP") mustBe Json.toJson("-")
      tags("path") mustBe Json.toJson("/register-for-paye/who-should-we-contact")
      tags("clientPort") mustBe Json.toJson("-")
      tags.contains("X-Session-ID") mustBe true
      tags.contains("X-Request-ID") mustBe true
      tags.contains("deviceID") mustBe true
      tags("Authorization") mustBe Json.toJson("-")
      tags("transactionName") mustBe Json.toJson("payeContactDetailsAmendment")
    }

    "not upsert the contact details in Business Registration and not send audit event if nothing has changed" in {
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
      val prepopContactJson =
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

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, prepopContactJson)

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)

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
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/where-to-send-post")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/$regId/contact-details")))
      reqPosts.size mustBe 0

      val reqPostsAudit = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      reqPostsAudit.size mustBe 0
    }
  }

  "GET Correspondence Address" should {
    val tradingName = "Foo Trading"
    val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
    val payeDoc =
      s"""{
         |"companyName": "$companyName",
         |"tradingName": "$tradingName",
         |"roAddress": $roDoc,
         |"ppobAddress": $roDoc,
         |"businessContactDetails": {}
         |}""".stripMargin

    val prepopContactJson =
      s"""
         |{
         |   "firstName": "Test",
         |   "middleName": "1",
         |   "surname": "last",
         |   "email": "test@email.com",
         |   "telephoneNumber": "01234567",
         |   "mobileNumber": "072343455"
         |}
       """.stripMargin

    "not be prepopulated if an error is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/business-registration/$regId/contact-details", 200, prepopContactJson)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/business-registration/$regId/addresses", 403, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val response = await(buildClient("/where-to-send-post")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Where should we send post about the company's PAYE?"
      document.getElementById("chosenAddress-roaddress").attr("value") mustBe "roAddress"
      document.getElementById("ro-address-line-1").text mustBe "1"
      document.getElementById("ro-address-line-2").text mustBe ", 2"
      document.getElementById("ro-post-code").text mustBe ", pc"

      an[Exception] mustBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))

      document.getElementById("chosenAddress-other").attr("value") mustBe "other"
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
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/business-registration/$regId/contact-details", 200, prepopContactJson)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val response = await(buildClient("/where-to-send-post")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      an[Exception] mustBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))
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
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/business-registration/$regId/contact-details", 200, prepopContactJson)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val response = await(buildClient("/where-to-send-post")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Where should we send post about the company's PAYE?"
      document.getElementById("chosenAddress-roaddress").attr("value") mustBe "roAddress"
      document.getElementById("chosenAddress-roaddress").attr("name") mustBe "chosenAddress"
      document.getElementById("ro-address-line-1").text mustBe "1"
      document.getElementById("ro-address-line-2").text mustBe ", 2"
      document.getElementById("ro-post-code").text mustBe ", pc"

      document.getElementById("chosenAddress-prepopaddress0").attr("value") mustBe "prepopAddress0"
      document.getElementById("chosenAddress-prepopaddress0").attr("name") mustBe "chosenAddress"
      document.getElementById("prepopaddress0-address-line-1").text mustBe "prepopLine1"
      document.getElementById("prepopaddress0-address-line-2").text mustBe ", prepopLine2"
      document.getElementById("prepopaddress0-post-code").text mustBe ", AB9 8ZZ"

      document.getElementById("chosenAddress-prepopaddress1").attr("value") mustBe "prepopAddress1"
      document.getElementById("chosenAddress-prepopaddress1").attr("name") mustBe "chosenAddress"
      document.getElementById("prepopaddress1-address-line-1").text mustBe "prepopLine11"
      document.getElementById("prepopaddress1-address-line-2").text mustBe ", prepopLine22"
      document.getElementById("prepopaddress1-address-line-3").text mustBe ", prepopLine33"
      document.getElementById("prepopaddress1-country").text mustBe ", prepopCountry"

      document.getElementById("chosenAddress-other").attr("value") mustBe "other"
      document.getElementById("chosenAddress-other").attr("name") mustBe "chosenAddress"
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
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-to-send-post").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/check-and-confirm-your-answers")

      val reqPosts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/$regId/contact-correspond-paye")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json mustBe Json.parse(updatedPayeDoc)
    }

    "return an error page when fail saving in PAYE Registration with a prepop address" in {
      val csrfToken = UUID.randomUUID().toString
      val addresses =
        s"""{
           |  "1": {
           |    "line1": "prepopLine11",
           |    "line2": "prepopLine22",
           |    "line3": "prepopLine33",
           |    "postCode": "prepopPC1"
           |  }
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 200, "")
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-to-send-post").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status mustBe 500
    }

    "send a correct Audit Event when roAddress has been chosen" in {
      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |  "companyName": "$companyName",
                      |  "tradingNAme": "testName",
                      |  "roAddress": $roDoc,
                      |  "ppobAddress": $roDoc,
                      |  "businessContactDetails": {
                      |    "email": "email@email.zzz",
                      |    "mobileNumber": "1234567890",
                      |    "phoneNumber": "0987654321"
                      |  }
                      |}""".stripMargin

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubS4LGet(regId, CacheKeys.CompanyDetails.toString, payeDoc)
      stubS4LGet(regId, CacheKeys.PAYEContact.toString, updatedPayeDoc)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")
      stubS4LPut(regId, CacheKeys.CompanyDetails.toString, dummyS4LResponse)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-to-send-post").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("roAddress")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/check-and-confirm-your-answers")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPosts.get(0)
      val json = Json.parse(captorPost.getBodyAsString)

      (json \ "auditSource").as[JsString].value mustBe "paye-registration-frontend"
      (json \ "auditType").as[JsString].value mustBe "correspondenceAddress"
      (json \ "detail" \ "externalUserId").as[JsString].value mustBe "Ext-xxx"
      (json \ "detail" \ "authProviderId").as[JsString].value mustBe "testAuthProviderId"
      (json \ "detail" \ "journeyId").as[JsString].value mustBe regId
      (json \ "detail" \ "addressUsed").as[JsString].value mustBe "RegisteredOffice"


    }

    "send a correct Audit Event when ppobAddress has been chosen" in {
      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |  "companyName": "$companyName",
                      |  "tradingNAme": "testName",
                      |  "roAddress": $roDoc,
                      |  "ppobAddress": $roDoc,
                      |  "businessContactDetails": {
                      |    "email": "email@email.zzz",
                      |    "mobileNumber": "1234567890",
                      |    "phoneNumber": "0987654321"
                      |  }
                      |}""".stripMargin

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubS4LGet(regId, CacheKeys.CompanyDetails.toString, payeDoc)
      stubS4LGet(regId, CacheKeys.PAYEContact.toString, updatedPayeDoc)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")
      stubS4LPut(regId, CacheKeys.CompanyDetails.toString, dummyS4LResponse)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-to-send-post").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("ppobAddress")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/check-and-confirm-your-answers")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPosts.get(0)
      val json = Json.parse(captorPost.getBodyAsString)
      (json \ "auditSource").as[JsString].value mustBe "paye-registration-frontend"
      (json \ "auditType").as[JsString].value mustBe "correspondenceAddress"
      (json \ "detail" \ "externalUserId").as[JsString].value mustBe "Ext-xxx"
      (json \ "detail" \ "authProviderId").as[JsString].value mustBe "testAuthProviderId"
      (json \ "detail" \ "journeyId").as[JsString].value mustBe regId
      (json \ "detail" \ "addressUsed").as[JsString].value mustBe "PrincipalPlaceOfBusiness"

      val tags = (json \ "tags").as[JsObject].value
      tags("clientIP") mustBe Json.toJson("-")
      tags("path") mustBe Json.toJson("/register-for-paye/where-to-send-post")
      tags("clientPort") mustBe Json.toJson("-")
      tags.contains("X-Session-ID") mustBe true
      tags.contains("X-Request-ID") mustBe true
      tags.contains("deviceID") mustBe true
      tags("Authorization") mustBe Json.toJson("-")
      tags("transactionName") mustBe Json.toJson("correspondenceAddress")
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
      stubSessionCacheMetadata(SessionId, regId)

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPatch(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/contact-correspond-paye", 200, updatedPayeDoc)
      stubPost(s"/business-registration/$regId/addresses", 200, newAddress2BusReg)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")
      stubGet(s"/api/confirmed\\?id\\=$addressLookupID", 200, addressFromALF)

      val response = await(buildClient(s"/return-from-address-for-corresp-addr?id=$addressLookupID")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/check-and-confirm-your-answers")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/$regId/addresses")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json mustBe Json.parse(newAddress2BusReg)
    }
  }
}