/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import common.exceptions.DownstreamExceptions
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.external._
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class CompanyRegistrationConnectorISpec extends IntegrationSpecBase {

  implicit val hc = HeaderCarrier()

  val regId = "12345"

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map(
      "microservice.services.company-registration.port" -> s"${WiremockHelper.wiremockPort}",
      "microservice.services.incorporation-frontend-stubs.port" -> s"${WiremockHelper.wiremockPort}",
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
    ))
    .build

  lazy val connector = app.injector.instanceOf[CompanyRegistrationConnector]

  val status = "SUCCESS"
  val txId = "txn1234"
  val paidIncorporation = "payt1234"
  val ackStatus = "ack1234"

  val companyRegistrationProfile = CompanyRegistrationProfile(
    status = status,
    transactionId = txId,
    ackRefStatus = Some(ackStatus),
    paidIncorporation = Some(paidIncorporation)
  )

  val companyRegistrationProfileJson = Json.obj(
    "status" -> status,
    "confirmationReferences" -> Json.obj(
      "payment-reference" -> paidIncorporation,
      "transaction-id" -> txId
    ),
    "acknowledgementReferences" -> Json.obj(
      "status" -> ackStatus
    )
  )

  "CompanyRegistrationConnector" when {

    Seq(false, true) foreach { useStub =>

      s"when using the stub is set to '$useStub'" when {

        def stubCompanyRegistrationDetails(status: Int, body: Option[JsValue] = None) = {

          val urlPath = if(useStub) {
            s"/incorporation-frontend-stubs/$regId/corporation-tax-registration"
          }  else {
            s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration"
          }

          val response = body match {
            case Some(value) => aResponse().withStatus(status).withBody(value.toString)
            case None => aResponse().withStatus(status)
          }

          stubFor(get(urlPathEqualTo(urlPath)).willReturn(response))
        }

        "calling .getCompanyRegistrationDetails(regId: String)" must {

          "handle a successful response returning the expected Company Registration Profile" in {

            await(buildClient(s"/test-only/feature-flag/companyRegistration/${!useStub}").get())

            stubCompanyRegistrationDetails(OK, Some(companyRegistrationProfileJson))

            val result = await(connector.getCompanyRegistrationDetails(regId))

            result mustBe companyRegistrationProfile
          }

          "handle a successful response but return a ConfirmationRefsNotFoundException when confirmationReferences is missing" in {

            await(buildClient(s"/test-only/feature-flag/companyRegistration/${!useStub}").get())

            stubCompanyRegistrationDetails(OK, Some(companyRegistrationProfileJson.-("confirmationReferences")))

            intercept[DownstreamExceptions.ConfirmationRefsNotFoundException](await(connector.getCompanyRegistrationDetails(regId)))
          }

          "handle any other response returning an CompanyRegistrationException" in {

            await(buildClient(s"/test-only/feature-flag/companyRegistration/${!useStub}").get())

            stubCompanyRegistrationDetails(INTERNAL_SERVER_ERROR, None)

            intercept[DownstreamExceptions.CompanyRegistrationException](await(connector.getCompanyRegistrationDetails(regId)))
          }
        }
      }
    }
  }
}
