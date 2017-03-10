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
package payeregistrationapi

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.CompanyRegistrationConnector
import itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.{Application, Play}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import services.MetricsService
import uk.gov.hmrc.play.http.HeaderCarrier

class CompanyRegistrationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.company-registration.uri" -> "/corporation-tax-registration",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val regId = "12345"
  implicit val hc = HeaderCarrier()

  val url = s"/corporation-tax-registration/$regId"

  "getCompanyRegistrationDetails" should {
    val testTransId =
      Json.parse(
        """
          |{
          |    "status" : "testStatus",
          |    "confirmationReferences" : {
          |      "transaction-id" : "testTransactionID-001"
          |    }
          |}
        """.stripMargin).as[JsObject]

    "get a status and a transaction id" in {
      lazy val metrics = Play.current.injector.instanceOf[MetricsService]
      val companyRegistrationConnector = new CompanyRegistrationConnector(metrics)

      def getResponse = companyRegistrationConnector.getCompanyRegistrationDetails(regId)

      stubFor(get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(testTransId).toString())
        )
      )

      val result = await(getResponse)
      result.status shouldBe "testStatus"
      result.transactionId shouldBe "testTransactionID-001"
    }
  }
}