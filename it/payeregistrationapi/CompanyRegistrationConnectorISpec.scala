/*
 * Copyright 2021 HM Revenue & Customs
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
import config.{AppConfig, WSHttpImpl}
import connectors.CompanyRegistrationConnectorImpl
import itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.{Application, Environment, Mode}
import services.MetricsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.PAYEFeatureSwitch

import scala.concurrent.ExecutionContext

class CompanyRegistrationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val config = Map(
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-frontend-stubs.host" -> s"$mockHost",
    "microservice.services.incorporation-frontend-stubs.port" -> s"$mockPort",
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
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
           |      "transaction-id" : "$transId",
           |      "payment-reference" : "paidcashmoney"
           |    }
           |}
        """.stripMargin).as[JsObject]

    def responseBodyUnpaid(transId: String) =
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
        lazy val http = app.injector.instanceOf(classOf[WSHttpImpl])
        lazy val appConfig = app.injector.instanceOf[AppConfig]
        lazy implicit val ec = app.injector.instanceOf[ExecutionContext]


        val companyRegistrationConnector = new CompanyRegistrationConnectorImpl(
          featureSwitch,
          http,
          metrics,
          appConfig
        )

        def getResponse = companyRegistrationConnector.getCompanyRegistrationDetails(regId)

        stubFor(get(urlMatching(stubUrl))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(responseBody("testTransactionID-001")).toString())
          )
        )

        val result = await(getResponse)
        result.status mustBe "testStatus"
        result.transactionId mustBe "testTransactionID-001"
        result.paidIncorporation mustBe Some("paidcashmoney")
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
        lazy val http = app.injector.instanceOf(classOf[WSHttpImpl])
        lazy val appConfig = app.injector.instanceOf[AppConfig]
        lazy implicit val ec = app.injector.instanceOf[ExecutionContext]


        val companyRegistrationConnector = new CompanyRegistrationConnectorImpl(
          featureSwitch,
          http,
          metrics,
          appConfig
        )

        await(buildClient("/test-only/feature-flag/companyRegistration/true").get())

        def getResponse = companyRegistrationConnector.getCompanyRegistrationDetails(regId)

        stubFor(get(urlMatching(url))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(responseBodyUnpaid("testTransactionID-001")).toString())
          )
        )

        val result = await(getResponse)
        result.status mustBe "testStatus"
        result.transactionId mustBe "testTransactionID-001"
        result.paidIncorporation mustBe None
      }
    }
  }
}