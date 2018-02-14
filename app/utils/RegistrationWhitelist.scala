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

package utils

import config.FrontendAppConfig
import connectors.{DESResponse, IncorpInfoResponse, IncorpInfoSuccessResponse}
import models.DigitalContactDetails
import models.api.{Director, CompanyDetails => CompanyDetailsAPI}
import models.external.{CoHoCompanyDetailsModel, CompanyRegistrationProfile, OfficerList}
import play.api.Logger

import scala.concurrent.Future
import scala.language.implicitConversions

trait RegistrationWhitelist {
  val applicationConfig = FrontendAppConfig

  implicit def getDefaultCompanyDetailsAPI(regId: String): Option[CompanyDetailsAPI] = Some(CompanyDetailsAPI(
    applicationConfig.defaultCompanyName,
    None,
    applicationConfig.defaultCHROAddress,
    applicationConfig.defaultCHROAddress,
    DigitalContactDetails(Some("email@email.com"),
      None,
      None
    )
  ))
  implicit def getDefaultSeqDirector(regId: String): Seq[Director] = applicationConfig.defaultSeqDirector
  implicit def getDefaultCompanyProfile(regId: String): CompanyRegistrationProfile =
    CompanyRegistrationProfile(applicationConfig.defaultCTStatus, s"fakeTxId-$regId", None)
  implicit def getDefaultCoHoCompanyDetails(regId: String): IncorpInfoResponse = IncorpInfoSuccessResponse(
    CoHoCompanyDetailsModel(
      applicationConfig.defaultCompanyName,
      applicationConfig.defaultCHROAddress
    )
  )
  implicit def cancelSubmission(regId: String): DESResponse = throw new Exception(s"Registration ID $regId is in whitelist, no submission allowed")

  implicit def getDefaultOfficerList(regId:String):OfficerList = applicationConfig.defaultOfficerList

  def ifRegIdNotWhitelisted[T](regId: String)(f: => Future[T])(implicit default: String => T): Future[T] = {
    if( applicationConfig.regIdWhitelist.contains(regId) ) {
      Logger.info(s"Registration ID $regId is in the whitelist")
      Future.successful(default(regId))
    } else {
      f
    }
  }
}
