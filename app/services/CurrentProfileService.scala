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

import connectors._
import enums.CacheKeys
import models.external.CurrentProfile
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CurrentProfileService @Inject()(injBusinessRegistrationConnector: BusinessRegistrationConnector,
                                      injKeystoreConnector: KeystoreConnector,
                                      injCompanyRegistrationConnector: CompanyRegistrationConnector) extends CurrentProfileSrv {

  override val businessRegistrationConnector = injBusinessRegistrationConnector
  override val companyRegistrationConnector = injCompanyRegistrationConnector
  override val keystoreConnector = injKeystoreConnector
}

trait CurrentProfileSrv {

  val businessRegistrationConnector: BusinessRegistrationConnect
  val companyRegistrationConnector: CompanyRegistrationConnect
  val keystoreConnector: KeystoreConnect

  def fetchAndStoreCurrentProfile(implicit hc: HeaderCarrier) : Future[CurrentProfile] = {
    for {
      businessProfile <- businessRegistrationConnector.retrieveCurrentProfile
      companyProfile  <- companyRegistrationConnector.getCompanyRegistrationDetails(businessProfile.registrationID)
      currentProfile = CurrentProfile(businessProfile.registrationID, businessProfile.completionCapacity, companyProfile, businessProfile.language)
      _ <- keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, currentProfile)
    } yield {
      currentProfile
    }
  }

}
