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

package connectors.httpParsers

import common.exceptions
import common.exceptions.DownstreamExceptions
import connectors.BaseConnector
import models.external.CompanyRegistrationProfile
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.mvc.Request
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.util.{Failure, Success, Try}

trait CompanyRegistrationHttpParsers extends BaseHttpReads { _: BaseConnector =>

  override def unexpectedStatusException(url: String, status: Int, regId: Option[String], txId: Option[String]): Exception =
    new exceptions.DownstreamExceptions.CompanyRegistrationException(s"Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")

  def companyRegistrationDetailsHttpReads(regId: String)(implicit request: Request[_]): HttpReads[CompanyRegistrationProfile] =
    (_: String, url: String, response: HttpResponse) =>
      response.status match {
        case OK =>
          Try(response.json.as[CompanyRegistrationProfile](CompanyRegistrationProfile.companyRegistrationReads)) match {
            case Success(profile) => profile
            case Failure(e: DownstreamExceptions.ConfirmationRefsNotFoundException) =>
              errorLog(s"[companyRegistrationDetailsHttpReads] Received an error when expecting a Company Registration document for reg id: $regId could not find confirmation references (has user completed Incorp/CT?)")
              throw e
            case Failure(e) =>
              errorLog(s"[companyRegistrationDetailsHttpReads] JSON returned from company-registration could not be parsed to CompanyRegistrationProfile model for reg id: $regId")
              throw e
          }
        case status =>
          unexpectedStatusHandling()("companyRegistrationDetailsHttpReads", url, status, Some(regId))
      }

  def verifiedEmailHttpReads(regId: String)(implicit request: Request[_]): HttpReads[Option[String]] =
    (_: String, url: String, response: HttpResponse) =>
      response.status match {
        case OK =>
          (response.json \ "address").asOpt[String] match {
            case Some(address) => Some(address)
            case None =>
              infoLog(s"[verifiedEmailHttpReads] A response for the verified email was returned but did not contain the 'address' object for regId: $regId")
              None
          }
        case NOT_FOUND =>
          infoLog(s"[verifiedEmailHttpReads] A call was made to company reg and a NotFound response was returned for regId: $regId")
          None
        case status =>
          unexpectedStatusHandling(Some(None))("verifiedEmailHttpReads", url, status, Some(regId))
      }
}

object CompanyRegistrationHttpParsers extends CompanyRegistrationHttpParsers with BaseConnector
