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

package frontend

import itutil._
import models.Address
import models.external._
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner

import java.util.UUID

class CompanyDetailsControllerISpec extends IntegrationSpecBase
  with LoginStub
  with CachingStub
  with BeforeAndAfterEach
  with WiremockHelper
  with RequestsFinder {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val config = Map(
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
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "regIdAllowlist" -> "cmVnQWxsb3dsaXN0MTIzLHJlZ0FsbG93bGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "defaultCHROAddress" -> "eyJsaW5lMSI6IjE0IFRlc3QgRGVmYXVsdCBTdHJlZXQiLCJsaW5lMiI6IlRlc3RsZXkiLCJsaW5lMyI6IlRlc3Rmb3JkIiwibGluZTQiOiJUZXN0c2hpcmUiLCJwb3N0Q29kZSI6IlRFMSAzU1QifQ==",
    "defaultSeqDirector" -> "W3siZGlyZWN0b3IiOnsiZm9yZW5hbWUiOiJmYXVsdHkiLCJzdXJuYW1lIjoiZGVmYXVsdCJ9fV0=",
    "mongodb.uri" -> s"$mongoUri"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  lazy val defaultCookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  override def beforeEach() {
    resetWiremock()
  }

  val testRegId: String = "testRegId"
  val testBusinessName: String = "test name"
  val testBusinessAddress: Address = Address("testLine1", "testLine2", None, None, Some("Z11 11Z"), Some("testCountry"))
  val companyName = "Foo Ltd"


  lazy val csrfToken: String = UUID.randomUUID().toString
  lazy val sessionCookie: String = getSessionCookie(Map("csrfToken" -> csrfToken))

  "POST PPOB Address" should {
    "save to microservice with full company details data" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      System.setProperty("feature.isWelsh", "true")

      stubPayeRegDocumentStatus(testRegId)

      val csrfToken = UUID.randomUUID().toString

      stubSessionCacheMetadata(SessionId, testRegId)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =
        s"""{
           |"companyName": "$companyName",
           |"tradingName": {"differentName":false},
           |"roAddress": $roDoc,
           |"businessContactDetails": {}
           |}""".stripMargin

      stubS4LGet(testRegId, "CompanyDetails", payeDoc)

      val updatedPayeDoc =
        s"""{
           |"companyName": "$companyName",
           |"tradingName": "tName",
           |"roAddress": $roDoc,
           |"ppobAddress": $roDoc,
           |"businessContactDetails": {}
           |}""".stripMargin
      stubPatch(s"/paye-registration/$testRegId/company-details", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/$testRegId", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))

      stubGet("controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress(None)", 200, "")

      stubPost(url = "/api/v2/init", 200, responseBody = "{}", ("Location", "/test")
      )

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("otherAddress")
        ))

      val response = await(fResponse)

      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/test")

      val onRampConfig: AlfJourneyConfig = getPOSTRequestJsonBody("/api/v2/init").as[AlfJourneyConfig]

      val expectedConfig: AlfJourneyConfig = AlfJourneyConfig(
        version = AlfJourneyConfig.defaultConfigVersion,
        options = JourneyOptions(
          continueUrl = "http://localhost:9870/register-for-paye/return-from-address-for-ppob",
          homeNavHref = "http://www.hmrc.gov.uk/",
          accessibilityFooterUrl = "http://localhost:9870/register-for-paye/accessibility-statement?pageUri=%2Fregister-for-paye%2F",
          deskProServiceName = "SCRS",
          showPhaseBanner = true,
          alphaPhase = false,
          showBackButtons = true,
          includeHMRCBranding = false,
          disableTranslations = false,


          selectPageConfig = SelectPageConfig(
            proposalListLimit = 30,
            showSearchAgainLink = true
          ),

          confirmPageConfig = ConfirmPageConfig(
            showSearchAgainLink = false,
            showSubHeadingAndInfo = false,
            showChangeLink = true
          ),

          timeoutConfig = TimeoutConfig(
            timeoutAmount = 900,
            timeoutUrl = "http://localhost:9870/register-for-paye/error/timeout"
          )
        ),
        labels = JourneyLabels(en = LanguageLabels(
          appLevelLabels = AppLevelLabels(
            navTitle = Some("Register an employer for PAYE"),
            phaseBannerHtml = Some("This is a new service. Help us improve it - send your <a href=\"https://www.tax.service.gov.uk/register-for-paye/feedback\">feedback</a>.")
          ),
          SelectPageLabels(
            title = Some("Choose an address"),
            heading = Some("Choose an address"),
            searchAgainLinkText = Some("Search again"),
            editAddressLinkText = Some("Edit address manually")
          ),
          LookupPageLabels(
            title = Some("Search for your address"),
            heading = Some("Search for your address"),
            filterLabel = Some("House name or number (optional)"),
            submitLabel = Some("Search address"),
            manualAddressLinkText = Some("The address doesn’t have a UK postcode")
          ),
          EditPageLabels(
            title = Some("Enter address"),
            heading = Some("Enter address"),
            line1Label = Some("Address line 1"),
            line2Label = Some("Address line 2"),
            line3Label = Some("Address line 3")
          ),
          ConfirmPageLabels(
            title = Some("Review and confirm your address"),
            heading = Some("Review and confirm your address"),
            submitLabel = Some("Save and continue"),
            changeLinkText = Some("Change")
          )
        ),
          cy = LanguageLabels(
            appLevelLabels = AppLevelLabels(
              navTitle = Some("Cofrestru cyflogwr ar gyfer TWE"),
              phaseBannerHtml = Some("Mae hwn yn wasanaeth newydd. Helpwch ni i’w wella – anfonwch eich adborth.")
            ),
            SelectPageLabels(
              title = Some("Dewiswch gyfeiriad"),
              heading = Some("Dewiswch gyfeiriad"),
              searchAgainLinkText = Some("Chwilio eto"),
              editAddressLinkText = Some("Golygwch y cyfeiriad â llaw")
            ),
            LookupPageLabels(
              title = Some("Chwiliwch am eich cyfeiriad"),
              heading = Some("Chwiliwch am eich cyfeiriad"),
              filterLabel = Some("Enw neu rif y tŷ (dewisol)"),
              submitLabel = Some("Chwilio am y cyfeiriad"),
              manualAddressLinkText = Some("Nid oes gan y cyfeiriad god post yn y DU")
            ),
            EditPageLabels(
              title = Some("Nodwch gyfeiriad"),
              heading = Some("Nodwch gyfeiriad"),
              line1Label = Some("Cyfeiriad - llinell 1"),
              line2Label = Some("Cyfeiriad - llinell 2"),
              line3Label = Some("Cyfeiriad - llinell 3")
            ),
            ConfirmPageLabels(
              title = Some("Adolygu a chadarnhau’ch cyfeiriad"),
              heading = Some("Adolygu a chadarnhau’ch cyfeiriad"),
              submitLabel = Some("Cadw ac yn eich blaen"),
              changeLinkText = Some("Newid")
            )
          )
        )
      )

      onRampConfig mustBe expectedConfig

    }
  }
}