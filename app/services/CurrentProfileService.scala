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

import connectors.{KeystoreConnector, BusinessRegistrationSuccessResponse, BusinessRegistrationConnector}
import enums.{DownstreamOutcome,CacheKeys}
import models.external.CurrentProfile
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object CurrentProfileService extends CurrentProfileService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val businessRegistrationConnector = BusinessRegistrationConnector
  //$COVERAGE-ON$

}

trait CurrentProfileService {

  val keystoreConnector: KeystoreConnector
  val businessRegistrationConnector: BusinessRegistrationConnector

  def fetchAndStoreCurrentProfile(implicit hc: HeaderCarrier) : Future[DownstreamOutcome.Value] = {
    businessRegistrationConnector.retrieveCurrentProfile flatMap {
      case BusinessRegistrationSuccessResponse(profile) =>
        val currentProfile = CurrentProfile(profile.registrationID, profile.completionCapacity, profile.language)
        keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, currentProfile).map { cacheMap =>
          DownstreamOutcome.Success
        } recover {
          case e => DownstreamOutcome.Failure
        }
      case _ => Future.successful(DownstreamOutcome.Failure)
    }
  }

}
