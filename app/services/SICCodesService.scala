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

import javax.inject.Inject

import connectors.{KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import enums.DownstreamOutcome
import models.api.SICCode
import models.view.NatureOfBusiness
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by henrilay on 22/02/2017.
  */
class SICCodesService @Inject()(
                                 keystoreConn: KeystoreConnector,
                                 payeRegistrationConn: PAYERegistrationConnector
                               ) extends SICCodesSrv {
  override val keystoreConnector = keystoreConn
  override val payeRegConnector = payeRegistrationConn
}

trait SICCodesSrv extends CommonService {
  val payeRegConnector: PAYERegistrationConnect

  private[services] def sicCodes2NatureOfBusiness(sicCodes: Seq[SICCode]): NatureOfBusiness =
    NatureOfBusiness(sicCodes.headOption.getOrElse(SICCode(None, None)).description.getOrElse(""))

  private[services] def natureOfBusiness2SICCodes(nob: NatureOfBusiness): Seq[SICCode] =
    Seq(SICCode(None, Some(nob.natureOfBusiness)))

  def getSICCodes()(implicit hc: HeaderCarrier): Future[NatureOfBusiness] = {
    for {
      regID <- fetchRegistrationID
      sicCodes <- payeRegConnector.getSICCodes(regID)
    } yield sicCodes2NatureOfBusiness(sicCodes)
  }

  def saveSICCodes(nob: NatureOfBusiness)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regID     <- fetchRegistrationID
      details   <- payeRegConnector.upsertSICCodes(regID, natureOfBusiness2SICCodes(nob))
    } yield DownstreamOutcome.Success
  }
}
