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

import enums.CacheKeys
import models.view.CompanyDetails
import play.api.mvc.Result
import services.{CompanyDetailsSrv, IncorporationInformationSrv, S4LSrv}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait UpToDateCompanyDetails {
  val incorpInfoService: IncorporationInformationSrv
  val companyDetailsService: CompanyDetailsSrv
  val s4LService: S4LSrv

  def withLatestCompanyDetails(regId: String, txId: String)(f: CompanyDetails => Result)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      oCoHoCompanyDetails <- incorpInfoService.getCompanyDetails(regId, txId) map(Some(_)) recover {case _ => None}
      companyDetails <- companyDetailsService.getCompanyDetails(regId, txId)
      details = oCoHoCompanyDetails.map(ch => companyDetails.copy(companyName = ch.companyName, roAddress = ch.roAddress)).getOrElse(companyDetails)
      _ <- s4LService.saveForm[CompanyDetails](CacheKeys.CompanyDetails.toString, details, regId)
    } yield f(details)
  }
}
