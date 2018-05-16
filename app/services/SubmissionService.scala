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

import javax.inject.Inject

import connectors._
import enums.CacheKeys
import models.external.CurrentProfile
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.RegistrationWhitelist

import scala.concurrent.Future

class SubmissionServiceImpl @Inject()(val payeRegistrationConnector: PAYERegistrationConnector,
                                      val keystoreConnector: KeystoreConnector) extends SubmissionService

trait SubmissionService extends RegistrationWhitelist {
  val payeRegistrationConnector: PAYERegistrationConnector
  val keystoreConnector: KeystoreConnector

  def submitRegistration(profile: CurrentProfile)(implicit hc: HeaderCarrier): Future[DESResponse] = {
    ifRegIdNotWhitelisted(profile.registrationID) {
      payeRegistrationConnector.submitRegistration(profile.registrationID).flatMap {
        case Success =>
          implicit val cp: CurrentProfile = profile.copy(payeRegistrationSubmitted = true)
          keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, cp).map {
            _ => Success
          }
        case other => Future.successful(other)
      }
    }
  }

}
