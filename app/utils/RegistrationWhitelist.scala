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

package utils

import config.FrontendAppConfig
import connectors.{IncorpInfoResponse, IncorpInfoSuccessResponse}
import models.DigitalContactDetails
import models.api.{Director, CompanyDetails => CompanyDetailsAPI}
import models.external.{CoHoCompanyDetailsModel, CompanyProfile}
import play.api.Logger

import scala.concurrent.Future

trait RegistrationWhitelist {
  val applicationConfig = FrontendAppConfig

  implicit def getDefaultCompanyDetailsAPI(regId: String): Option[CompanyDetailsAPI] =
    Some(CompanyDetailsAPI(applicationConfig.defaultCompanyName, None, applicationConfig.defaultCHROAddress, applicationConfig.defaultCHROAddress, DigitalContactDetails(None, None, None)))
  implicit def getDefaultSeqDirector(regId: String): Seq[Director] = applicationConfig.defaultSeqDirector
  implicit def getDefaultCompanyProfile(regId: String): CompanyProfile = CompanyProfile(applicationConfig.defaultCTStatus, s"fakeTxId-$regId")
  implicit def getDefaultCoHoCompanyDetails(regId: String): IncorpInfoResponse = IncorpInfoSuccessResponse(CoHoCompanyDetailsModel(regId, applicationConfig.defaultCompanyName, Seq.empty))

  def ifRegIdNotWhitelisted[T](regId: String)(f: => Future[T])(implicit default: String => T): Future[T] = {
    if( applicationConfig.regIdWhitelist.contains(regId) ) {
      Logger.info(s"Registration ID $regId is in the whitelist")
      Future.successful(default(regId))
    } else {
      f
    }
  }
}
