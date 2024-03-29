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

import config.AppConfig
import connectors._
import enums.{CacheKeys, IncorporationStatus}
import models.external.CurrentProfile
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import utils.RegistrationAllowlist

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionService @Inject()(val payeRegistrationConnector: PAYERegistrationConnector,
                                      val keystoreConnector: KeystoreConnector,
                                      val iiConnector: IncorporationInformationConnector
                                     )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends RegistrationAllowlist {

  def submitRegistration(profile: CurrentProfile)(implicit hc: HeaderCarrier, request: Request[_]): Future[DESResponse] = {
    ifRegIdNotAllowlisted(profile.registrationID) {
      payeRegistrationConnector.submitRegistration(profile.registrationID).flatMap {
        case status@(Success | Cancelled) =>
          val cp: CurrentProfile = if (status == Success) profile.copy(payeRegistrationSubmitted = true) else profile.copy(incorpStatus = Some(IncorporationStatus.rejected))

          keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, profile.registrationID, profile.companyTaxRegistration.transactionId, cp).flatMap {
            sessionMap => iiConnector.cancelSubscription(sessionMap.transactionId, sessionMap.registrationId).map { _ => status }
          }
        case other => Future.successful(other)
      }
    }
  }

}
