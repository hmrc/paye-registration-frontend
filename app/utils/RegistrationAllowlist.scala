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

package utils

import config.AppConfig
import connectors.{DESResponse, IncorpInfoResponse, IncorpInfoSuccessResponse}
import models.DigitalContactDetails
import models.api.{Director, CompanyDetails => CompanyDetailsAPI}
import models.external.{CoHoCompanyDetailsModel, CompanyRegistrationProfile, OfficerList}
import play.api.mvc.Request

import scala.concurrent.Future
import scala.language.implicitConversions

trait RegistrationAllowlist extends Logging {
  val appConfig: AppConfig

  implicit def getDefaultCompanyDetailsAPI(regId: String): Option[CompanyDetailsAPI] = Some(CompanyDetailsAPI(
    appConfig.defaultCompanyName,
    None,
    appConfig.defaultCHROAddress,
    appConfig.defaultCHROAddress,
    DigitalContactDetails(Some("email@email.com"),
      None,
      None
    )
  ))

  implicit def getDefaultSeqDirector(regId: String): Seq[Director] = appConfig.defaultSeqDirector

  implicit def getDefaultCompanyProfile(regId: String): CompanyRegistrationProfile =
    CompanyRegistrationProfile(appConfig.defaultCTStatus, s"fakeTxId-$regId", None)

  implicit def getDefaultCoHoCompanyDetails(regId: String): IncorpInfoResponse = IncorpInfoSuccessResponse(
    CoHoCompanyDetailsModel(
      appConfig.defaultCompanyName,
      appConfig.defaultCHROAddress
    )
  )

  implicit def cancelSubmission(regId: String): DESResponse = throw new Exception(s"Registration ID $regId is in allow-list, no submission allowed")

  implicit def getDefaultOfficerList(regId: String): OfficerList = appConfig.defaultOfficerList

  def ifRegIdNotAllowlisted[T](regId: String)(f: => Future[T])(implicit default: String => T, request: Request[_]): Future[T] = {
    if (appConfig.regIdAllowlist.contains(regId)) {
      infoLog(s"[ifRegIdNotAllowlisted] Registration ID $regId is in the allow-list")
      Future.successful(default(regId))
    } else {
      f
    }
  }
}
