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

package payeregistrationapi

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.CompanyRegistrationConnector
import itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.{Application, Play}
import services.MetricsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.PAYEFeatureSwitch

class CompanyRegistrationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-frontend-stubs.host" -> s"$mockHost",
    "microservice.services.incorporation-frontend-stubs.port" -> s"$mockPort",
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val regId = "12345"
  implicit val hc = HeaderCarrier()

  val stubUrl = s"/incorporation-frontend-stubs/$regId/corporation-tax-registration"
  val url = s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration"

  "getCompanyRegistrationDetails" should {
    def responseBody(transId: String) =
      Json.parse(
        s"""
          |{
          |    "status" : "testStatus",
          |    "confirmationReferences" : {
          |      "transaction-id" : "$transId"
          |    }
          |}
        """.stripMargin).as[JsObject]

    "get a status and a transaction id" when {

      "the feature flag points at the stub" in {
        lazy val metrics = app.injector.instanceOf[MetricsService]
        lazy val featureSwitch = app.injector.instanceOf[PAYEFeatureSwitch]
        val companyRegistrationConnector = new CompanyRegistrationConnector(featureSwitch, metrics)

        def getResponse = companyRegistrationConnector.getCompanyRegistrationDetails(regId)

        stubFor(get(urlMatching(stubUrl))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(responseBody("testTransactionID-001")).toString())
          )
        )

        val result = await(getResponse)
        result.status shouldBe "testStatus"
        result.transactionId shouldBe "testTransactionID-001"
      }

      "the feature flag points at the Company Registration" in {
        stubFor(post(urlMatching("/write/audit"))
          .willReturn(
            aResponse().
              withStatus(200).
              withBody("""{"x":2}""")
          )
        )

        lazy val metrics = app.injector.instanceOf[MetricsService]
        lazy val featureSwitch = app.injector.instanceOf[PAYEFeatureSwitch]
        val companyRegistrationConnector = new CompanyRegistrationConnector(featureSwitch, metrics)

        await(buildClient("/test-only/feature-flag/companyRegistration/true").get())

        def getResponse = companyRegistrationConnector.getCompanyRegistrationDetails(regId)

        stubFor(get(urlMatching(url))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(responseBody("testTransactionID-001")).toString())
          )
        )

        val result = await(getResponse)
        result.status shouldBe "testStatus"
        result.transactionId shouldBe "testTransactionID-001"

        await(buildClient("/test-only/feature-flag/companyRegistration/false").get())

        def getStubResponse = companyRegistrationConnector.getCompanyRegistrationDetails(regId)

        stubFor(get(urlMatching(stubUrl))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(responseBody("testTransactionID-002")).toString())
          )
        )

        val stubbedResult = await(getResponse)
        stubbedResult.status shouldBe "testStatus"
        stubbedResult.transactionId shouldBe "testTransactionID-002"
      }
    }
  }
}