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

import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import enums.{CacheKeys, DownstreamOutcome, UserCapacity}
import models.external.CurrentProfile
import models.view.CompletionCapacity
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CompletionCapacityService @Inject()(
                                           injPAYERegistrationConnector: PAYERegistrationConnector,
                                           injKeystoreConnector: KeystoreConnector
                                         ) extends CompletionCapacitySrv {

  override val payeRegConnector = injPAYERegistrationConnector
  override val keystoreConnector = injKeystoreConnector
}

trait CompletionCapacitySrv {

  val payeRegConnector: PAYERegistrationConnect
  val keystoreConnector: KeystoreConnect


  def saveCompletionCapacity(completionCapacity: CompletionCapacity, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    payeRegConnector.upsertCompletionCapacity(regId, viewToAPI(completionCapacity)) map {
      _ => DownstreamOutcome.Success
    }
  }

  def getCompletionCapacity(regId: String)(implicit hc: HeaderCarrier): Future[Option[CompletionCapacity]] = {
    for {
      oCapacity <- payeRegConnector.getCompletionCapacity(regId)
      capacity  <- convertOrFetchCapacity(oCapacity)
    } yield capacity.map(apiToView)
  }

  def convertOrFetchCapacity(oCapacity:Option[String])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    oCapacity match {
      case Some(capacity) => Future.successful(Some(capacity))
      case None           => getCapacityFromKeystore
    }
  }

  def getCapacityFromKeystore()(implicit hc: HeaderCarrier): Future[Option[String]] = {
    keystoreConnector.fetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString).map{
      oCurrentProfile => oCurrentProfile.flatMap(_.completionCapacity)
    }
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
