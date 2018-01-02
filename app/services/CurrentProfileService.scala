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

import connectors._
import enums.{CacheKeys, PAYEStatus}
import models.external.CurrentProfile
import uk.gov.hmrc.http.HeaderCarrier
import utils.RegistrationWhitelist

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

@Singleton
class CurrentProfileService @Inject()(val businessRegistrationConnector: BusinessRegistrationConnector,
                                      val payeRegistrationConnector: PAYERegistrationConnector,
                                      val keystoreConnector: KeystoreConnector,
                                      val companyRegistrationConnector: CompanyRegistrationConnector) extends CurrentProfileSrv

trait CurrentProfileSrv extends RegistrationWhitelist {

  val businessRegistrationConnector: BusinessRegistrationConnect
  val payeRegistrationConnector: PAYERegistrationConnect
  val companyRegistrationConnector: CompanyRegistrationConnect
  val keystoreConnector: KeystoreConnect

  def fetchAndStoreCurrentProfile(implicit hc: HeaderCarrier) : Future[CurrentProfile] = {
    for {
      businessProfile <- businessRegistrationConnector.retrieveCurrentProfile
      companyProfile  <- ifRegIdNotWhitelisted(businessProfile.registrationID) {
        companyRegistrationConnector.getCompanyRegistrationDetails(businessProfile.registrationID)
      }
      oRegStatus      <- payeRegistrationConnector.getStatus(businessProfile.registrationID)
      submitted       =  regSubmitted(oRegStatus)
      currentProfile  =  CurrentProfile(businessProfile.registrationID, companyProfile, businessProfile.language, submitted)
      _               <- keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, currentProfile)
    } yield {
      currentProfile
    }
  }

  private[services] def regSubmitted(oRegStatus: Option[PAYEStatus.Value]): Boolean = {
    oRegStatus.exists {
      case PAYEStatus.draft | PAYEStatus.invalid => false
      case _                                     => true
    }
  }
}
