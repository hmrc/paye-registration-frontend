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

package services

import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import models.api.SICCode
import models.view.NatureOfBusiness
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NatureOfBusinessService @Inject()(val payeRegConnector: PAYERegistrationConnector)(implicit val ec: ExecutionContext)  {

  private[services] def sicCodes2NatureOfBusiness(sicCodes: Seq[SICCode]): Option[NatureOfBusiness] =
    sicCodes.headOption.flatMap(_.description.map(NatureOfBusiness(_)))

  private[services] def natureOfBusiness2SICCodes(nob: NatureOfBusiness): Seq[SICCode] =
    Seq(SICCode(None, Some(nob.natureOfBusiness)))

  def getNatureOfBusiness(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[NatureOfBusiness]] = {
    for {
      sicCodes <- payeRegConnector.getSICCodes(regId)
    } yield sicCodes2NatureOfBusiness(sicCodes)
  }

  def saveNatureOfBusiness(nob: NatureOfBusiness, regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[DownstreamOutcome.Value] = {
    for {
      details <- payeRegConnector.upsertSICCodes(regId, natureOfBusiness2SICCodes(nob))
    } yield DownstreamOutcome.Success
  }
}
