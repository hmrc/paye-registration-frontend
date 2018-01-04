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

package connectors

import mocks.MockMetrics
import models.external.Ticket
import play.api.libs.json.{JsObject, Json}
import services.MetricsSrv
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

class DeskproConnectorSpec extends PAYERegSpec {

  class Setup {
    val connector = new DeskproConnect {
      override val deskProUrl: String = "testUrl"
      override val http: WSHttp = mockWSHttp
      override val metricsService: MetricsSrv = new MockMetrics
    }

    implicit val hc = HeaderCarrier()
  }

  val ticket: Ticket = Ticket(
    name = "Mr Bobby B. Bobblington III MBE BSc",
    email = "thebigb@testmail.test",
    subject = "payeReg DeskPro ticket",
    message = "testMessage",
    referrer = "testUrl",
    javascriptEnabled = "Y",
    userAgent = "paye-registration-frontend",
    authId = "test/test/test",
    areaOfTax = "unknown",
    sessionId = "testsession-123456",
    service = "scrs"
  )

  val ticketNum = 123456789
  val response : JsObject = Json.obj("ticket_id" -> ticketNum)

  "submitTicket" should {
    "return a ticket number" in new Setup {
      mockHttpPOST[Ticket, JsObject](s"${connector.deskProUrl}/deskpro/ticket", Future.successful(response))

      await(connector.submitTicket(ticket)) shouldBe ticketNum
    }

    "throw a bad request exception" in new Setup {
      mockHttpFailedPOST[Ticket, JsObject](s"${connector.deskProUrl}/deskpro/ticket", new BadRequestException("404"))

      intercept[BadRequestException](await(connector.submitTicket(ticket)))
    }

    "throw any other exception" in new Setup {
      mockHttpFailedPOST[Ticket, JsObject](s"${connector.deskProUrl}/deskpro/ticket", new RuntimeException)

      intercept[RuntimeException](await(connector.submitTicket(ticket)))
    }
  }
}
