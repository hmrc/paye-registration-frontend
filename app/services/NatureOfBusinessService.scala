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

package services

import javax.inject.{Inject, Singleton}

import connectors.{KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import enums.DownstreamOutcome
import models.api.SICCode
import models.view.NatureOfBusiness
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NatureOfBusinessService @Inject()(payeRegistrationConn: PAYERegistrationConnector) extends NatureOfBusinessSrv {
  override val payeRegConnector = payeRegistrationConn
}

trait NatureOfBusinessSrv {
  val payeRegConnector: PAYERegistrationConnect

  private[services] def sicCodes2NatureOfBusiness(sicCodes: Seq[SICCode]): Option[NatureOfBusiness] =
    sicCodes.headOption.flatMap(_.description.map(NatureOfBusiness(_)))

  private[services] def natureOfBusiness2SICCodes(nob: NatureOfBusiness): Seq[SICCode] =
    Seq(SICCode(None, Some(nob.natureOfBusiness)))

  def getNatureOfBusiness(regId: String)(implicit hc: HeaderCarrier): Future[Option[NatureOfBusiness]] = {
    for {
      sicCodes <- payeRegConnector.getSICCodes(regId)
    } yield sicCodes2NatureOfBusiness(sicCodes)
  }

  def saveNatureOfBusiness(nob: NatureOfBusiness, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details   <- payeRegConnector.upsertSICCodes(regId, natureOfBusiness2SICCodes(nob))
    } yield DownstreamOutcome.Success
  }
}
