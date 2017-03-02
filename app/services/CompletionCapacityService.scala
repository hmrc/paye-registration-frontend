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

import connectors.{PAYERegistrationConnector, PAYERegistrationConnect, KeystoreConnector}
import enums.{DownstreamOutcome, UserCapacity}
import models.view.CompletionCapacity
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CompletionCapacityService @Inject()(injKeystoreConnector: KeystoreConnector,
                                          injPAYERegistrationConnector: PAYERegistrationConnector
                                           ) extends CompletionCapacitySrv {

  override val keystoreConnector = injKeystoreConnector
  override val payeRegConnector = injPAYERegistrationConnector
}

trait CompletionCapacitySrv extends CommonService {

  val payeRegConnector: PAYERegistrationConnect

  def saveCompletionCapacity(completionCapacity: CompletionCapacity)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regId   <- fetchRegistrationID
      outcome <- payeRegConnector.upsertCompletionCapacity(regId, viewToAPI(completionCapacity))
    } yield DownstreamOutcome.Success
  }

  def getCompletionCapacity()(implicit hc: HeaderCarrier): Future[Option[CompletionCapacity]] = {
    for {
      regId <- fetchRegistrationID
      resp  <- payeRegConnector.getCompletionCapacity(regId)
    } yield resp.map(apiToView)
  }

  private[services] def viewToAPI(completionCapacity: CompletionCapacity): String = {
    completionCapacity.completionCapacity match {
      case UserCapacity.director => UserCapacity.director.toString
      case UserCapacity.agent    => UserCapacity.agent.toString
      case UserCapacity.other    => completionCapacity.completionCapacityOther
    }
  }

  private[services] def apiToView(completionCapacity: String): CompletionCapacity = {
    completionCapacity.toLowerCase match {
      case "director" => CompletionCapacity(UserCapacity.director, "")
      case "agent"    => CompletionCapacity(UserCapacity.agent, "")
      case _          => CompletionCapacity(UserCapacity.other, completionCapacity)
    }
  }
}