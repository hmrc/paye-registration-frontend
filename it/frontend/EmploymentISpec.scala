
package frontend

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.test.FakeApplication

class EmploymentISpec extends IntegrationSpecBase with LoginStub with CachingStub with BeforeAndAfterEach with WiremockHelper {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
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
  ))

  def setSystemDate() = {
    stubGet(s"/paye-registration/test-only/feature-flag/system-date/2018-07-12T00:00:00Z", 200, "")
    buildClient("/test-only/feature-flag/system-date/2018-07-12T00:00:00").get()
  }

  override def beforeEach() {
    resetWiremock()
  }
  val regId = "3"
  val txId  = "12345"

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
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
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
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 200

      val doc = Jsoup.parse(response.body)
      doc.getElementById("alreadyPaying-true").attr("checked") mustBe "checked"
      doc.getElementById("earliestDateDay").attr("value") mustBe "21"
      doc.getElementById("earliestDateMonth").attr("value") mustBe "5"
      doc.getElementById("earliestDateYear").attr("value") mustBe "2017"
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
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
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
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "alreadyPaying" -> Seq("true"),
          "earliestDateDay" -> Seq("21"),
          "earliestDateMonth" -> Seq("05"),
          "earliestDateYear"  -> Seq("2017")
        ))
      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry().url)

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
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "alreadyPaying" -> Seq("true"),
          "earliestDateDay" -> Seq("21"),
          "earliestDateMonth" -> Seq("05"),
          "earliestDateYear"  -> Seq("2017")
        ))
      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.NewEmploymentController.employingStaff().url)
    }

    "return 400" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/incorporation-information/$txId/incorporation-update", 200, incorpUpdateSuccessResponse)

      val fResponse = buildClient("/employ-anyone").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "alreadyPaying" -> Seq("true"),
          "earliestDateDay" -> Seq(""),
          "earliestDateMonth" -> Seq("05"),
          "earliestDateYear"  -> Seq("2017")
        ))
      val response = await(fResponse)
      response.status mustBe 400
      val doc = Jsoup.parse(response.body)
      doc.getElementById("earliestDate-fieldset-error-summary").html mustBe "Enter a date for when you first started employing someone or providing expenses or benefits to staff"
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
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get()

      val response = await(fResponse)
      response.status mustBe 200

      val doc = Jsoup.parse(response.body)

      doc.getElementById("willBePaying-true").attr("checked") mustBe "checked"
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
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "willBePaying" -> Seq("false")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry().url)
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
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx")
        ))

      val response = await(fResponse)
      response.status mustBe 400
      val doc = Jsoup.parse(response.body)
      doc.getElementById("willBePaying-error-summary").html mustBe "Tell us if, over the next 2 months, the company will employ anyone or provide expenses or benefits to staff"
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
          withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck").
          post(Map(
            "csrfToken"->Seq("xxx-ignored-xxx"),
            "paysPension" -> Seq("false")
          ))

        val response = await(fResponse)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity().url)
      }
    }
}
