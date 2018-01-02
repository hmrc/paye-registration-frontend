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

package services

import javax.inject.{Inject, Singleton}

import config.FrontendAuthConnector
import connectors.{DeskproConnect, DeskproConnector}
import models.external.Ticket
import models.view.{Ticket => TicketForm}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class DeskproService @Inject()(val deskproConnector: DeskproConnector) extends DeskproSrv {
  override val authConnector = FrontendAuthConnector
}

trait DeskproSrv {
  val authConnector : AuthConnector
  val deskproConnector : DeskproConnect

  private[services] def getAuthId(implicit hc: HeaderCarrier) : Future[String] = authConnector.currentAuthority.map(res => res.get.uri)

  private[services] def getSessionId(implicit hc: HeaderCarrier) : String = hc.sessionId.map(_.value).getOrElse("n/a")

  private[services] def buildTicket(regId: String, data: TicketForm)(implicit hc: HeaderCarrier) : Future[Ticket] = {
    getAuthId map { aId =>
      Ticket(
        data.name,
        data.email,
        subject = s"PAYE Registration submission failed for Registration ID: $regId",
        data.message,
        referrer = "https://www.tax.service.gov.uk/register-for-paye",
        javascriptEnabled = "Y",
        userAgent = "paye-registration-frontend",
        authId = aId,
        areaOfTax = "unknown",
        sessionId = getSessionId,
        service = "SCRS"
      )
    }
  }

  def submitTicket(regId: String, data: TicketForm)(implicit hc: HeaderCarrier) : Future[Long] = {
    buildTicket(regId, data) flatMap { ticket =>
      deskproConnector.submitTicket(ticket)
    }
  }
}
