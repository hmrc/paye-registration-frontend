/*
 * Copyright 2019 HM Revenue & Customs
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
import enums.{CacheKeys, IncorporationStatus, PAYEStatus}
import models.api.SessionMap
import models.external.CurrentProfile
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.RegistrationWhitelist

import scala.concurrent.Future

class CurrentProfileServiceImpl @Inject()(val businessRegistrationConnector: BusinessRegistrationConnector,
                                          val payeRegistrationConnector: PAYERegistrationConnector,
                                          val keystoreConnector: KeystoreConnector,
                                          val companyRegistrationConnector: CompanyRegistrationConnector,
                                          val incorporationInformationConnector: IncorporationInformationConnector) extends CurrentProfileService

trait CurrentProfileService extends RegistrationWhitelist {

  val businessRegistrationConnector: BusinessRegistrationConnector
  val payeRegistrationConnector: PAYERegistrationConnector
  val companyRegistrationConnector: CompanyRegistrationConnector
  val keystoreConnector: KeystoreConnector
  val incorporationInformationConnector: IncorporationInformationConnector

  def fetchAndStoreCurrentProfile(implicit hc: HeaderCarrier) : Future[CurrentProfile] = {
    for {
      businessProfile <- businessRegistrationConnector.retrieveCurrentProfile
      companyProfile  <- ifRegIdNotWhitelisted(businessProfile.registrationID) {
        companyRegistrationConnector.getCompanyRegistrationDetails(businessProfile.registrationID)
      }
      oRegStatus      <- payeRegistrationConnector.getStatus(businessProfile.registrationID)
      submitted       =  regSubmitted(oRegStatus)
      incorpStatus    <- incorporationInformationConnector.setupSubscription(companyProfile.transactionId,businessProfile.registrationID)
      currentProfile  =  CurrentProfile(businessProfile.registrationID, companyProfile, businessProfile.language, submitted, incorpStatus)
      _               <- keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, businessProfile.registrationID, companyProfile.transactionId, currentProfile)
    } yield {
      currentProfile
    }
  }

  private def updateSessionMap(sessionMap: Option[SessionMap], status: IncorporationStatus.Value): Option[SessionMap] = sessionMap.map {
    session =>
      val updatedCp = session.getEntry[CurrentProfile](CacheKeys.CurrentProfile.toString).map(_.copy(incorpStatus = Some(status)))
      session.copy(data = Map(CacheKeys.CurrentProfile.toString -> Json.toJson(updatedCp)))
  }

  def updateCurrentProfileWithIncorpStatus(txId: String, status: IncorporationStatus.Value)(implicit hc: HeaderCarrier):Future[Option[String]] =  for {
    updatedSessionMap <- keystoreConnector.fetchByTransactionId(txId).map(updateSessionMap(_, status))
    _                 = updatedSessionMap.map(sessionMap => keystoreConnector.cacheSessionMap(sessionMap))
    regId             = updatedSessionMap.flatMap(_.getEntry[CurrentProfile](CacheKeys.CurrentProfile.toString).map(_.registrationID))
  } yield regId

  private[services] def regSubmitted(oRegStatus: Option[PAYEStatus.Value]): Boolean = {
    oRegStatus.exists {
      case PAYEStatus.draft | PAYEStatus.invalid => false
      case _                                     => true
    }
  }
}
