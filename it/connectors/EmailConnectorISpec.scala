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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.external._
import models.{EmailDifficulties, EmailSent}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class EmailConnectorISpec extends IntegrationSpecBase {

  implicit val hc = HeaderCarrier()

  val regId = "12345"

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map("microservice.services.email.sendAnEmailURL" -> s"${WiremockHelper.url}/hmrc/email"))
    .build

  lazy val connector = app.injector.instanceOf[EmailConnector]

  "EmailConnector" when {

    "calling .requestEmailToBeSent()" must {

      val emailRequest = EmailRequest(
        to = Seq("test@example.com"),
        templateId = "fooTemplate",
        parameters = Map("foo" -> "bar"),
        force = true
      )
      val emailRequestJson = Json.toJson(emailRequest)

      def stubRequestEmailToBeSent(emailNotice: JsValue)(status: Int) = {
        stubFor(
          post(urlPathEqualTo(s"/hmrc/email"))
            .withRequestBody(equalToJson(emailNotice.toString))
            .willReturn(aResponse().withStatus(status))
        )
      }

      "handle a successful response returning an EmailSent ADT" in {

        stubRequestEmailToBeSent(emailRequestJson)(OK)

        val result = await(connector.requestEmailToBeSent(emailRequest))

        result mustBe EmailSent
      }

      "handle a NOT_FOUND returning EmailDifficulties ADT" in {

        stubRequestEmailToBeSent(emailRequestJson)(NOT_FOUND)

        val result = await(connector.requestEmailToBeSent(emailRequest))

        result mustBe EmailDifficulties
      }

      "handle any other response returning an EmailDifficulties ADT" in {

        stubRequestEmailToBeSent(emailRequestJson)(INTERNAL_SERVER_ERROR)

        val result = await(connector.requestEmailToBeSent(emailRequest))

        result mustBe EmailDifficulties
      }
    }

  }
}
