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

import enums.DownstreamOutcome
import connectors._
import models.api.{PAYERegistration => PAYERegistrationAPI}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PAYERegistrationService extends PAYERegistrationService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val payeRegistrationConnector = PAYERegistrationConnector
  override val s4LService = S4LService
  //$COVERAGE-ON$
}

trait PAYERegistrationService extends CommonService {

  val payeRegistrationConnector: PAYERegistrationConnector
  val s4LService: S4LService

  def createNewRegistration()(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegistrationConnector.createNewRegistration(regID)
    } yield regResponse match {
      case PAYERegistrationSuccessResponse(reg: PAYERegistrationAPI) => DownstreamOutcome.Success
      case _ => DownstreamOutcome.Failure
    }
  }
}
