
package filters

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.test.FakeApplication

class PAYESessionIDFilterISpec extends IntegrationSpecBase
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
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort"))



  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"

  "Loading a page" should {
    "redirect to post-sign-in" when {
      "sessionId is invalid" in {
        setupSimpleAuthMocks()
        stubSuccessfulLogin()
        stubKeystoreMetadata(SessionId, regId)

        stubGet(s"/paye-registration/$regId/company-details", 404, "")
        stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
        stubGet(s"/business-registration/$regId/contact-details", 403, "")
        stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
        val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

        val response = await(buildClient("/who-should-we-contact")
          .withHeaders(HeaderNames.COOKIE -> getSessionCookie(sessionID = invalidSessionId))
          .get())

        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/post-sign-in")
      }
      "user is not authorised" in {
        setupUnauthorised()

        val response = await(buildClient("/who-should-we-contact")
          .withHeaders(HeaderNames.COOKIE -> getSessionCookie(sessionID = invalidSessionId))
          .get())

        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/post-sign-in")
      }
    }
    "continue as normal" when {
      "sessionId is Valid" in {
        setupSimpleAuthMocks()
        stubSuccessfulLogin()
        stubKeystoreMetadata(SessionId, regId)

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
    }
  }
}
