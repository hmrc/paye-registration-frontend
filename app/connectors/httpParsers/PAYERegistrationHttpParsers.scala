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
import connectors.{BaseConnector, Cancelled, DESResponse, TimedOut, Success => DESSuccess}
import enums.{DownstreamOutcome, PAYEStatus, RegistrationDeletion}
import models.api._
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

trait PAYERegistrationHttpParsers extends BaseHttpReads { _: BaseConnector =>

  override def unexpectedStatusException(url: String, status: Int, regId: Option[String], txId: Option[String]): Exception =
    new exceptions.DownstreamExceptions.PAYEMicroserviceException(s"Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")

  def createNewRegistrationHttpReads(regId: String, transactionId: String): HttpReads[DownstreamOutcome.Value] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => DownstreamOutcome.Success
      case status =>
        unexpectedStatusHandling(Some(DownstreamOutcome.Failure))("createNewRegistrationHttpReads", url, status, Some(regId), Some(transactionId))
    }

  def getRegistrationHttpReads(regId: String): HttpReads[PAYERegistration] =
    httpReads("getRegistrationHttpReads", Some(regId))

  def getRegistrationIdHttpReads(txId: String): HttpReads[String] =
    httpReads("getRegistrationIdHttpReads", txId = Some(txId))

  def submitRegistrationHttpReads(regId: String): HttpReads[DESResponse] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => DESSuccess
      case NO_CONTENT => Cancelled
      case GATEWAY_TIMEOUT | REQUEST_TIMEOUT  =>
        logger.error("[submitRegistrationHttpReads] Timed out when submitting PAYE Registration to DES" + logContext(Some(regId)))
        TimedOut
      case status =>
        unexpectedStatusHandling()("submitRegistrationHttpReads", url, status, Some(regId))
    }

  def getCompanyDetailsHttpReads(regId: String): HttpReads[Option[CompanyDetails]] =
    optionHttpReads("getCompanyDetailsHttpReads", Some(regId))

  def upsertCompanyDetailsHttpReads(regId: String): HttpReads[CompanyDetails] =
    httpReads("upsertCompanyDetailsHttpReads", Some(regId))

  def getEmploymentHttpReads(regId: String): HttpReads[Option[Employment]] =
    optionHttpReads("getEmploymentHttpReads", Some(regId))

  def upsertEmploymentHttpReads(regId: String): HttpReads[Employment] =
    httpReads("upsertEmploymentHttpReads", Some(regId))

  def directorsHttpReads(regId: String): HttpReads[Seq[Director]] =
    seqHttpReads("directorsHttpReads", Some(regId))

  def sicCodesHttpReads(regId: String): HttpReads[Seq[SICCode]] =
    seqHttpReads("sicCodesHttpReads", Some(regId))

  def getPAYEContactHttpReads(regId: String): HttpReads[Option[PAYEContact]] =
    optionHttpReads("getPAYEContactHttpReads", Some(regId))

  def upsertPAYEContactHttpReads(regId: String): HttpReads[PAYEContact] =
    httpReads("upsertPAYEContactHttpReads", Some(regId))

  def getCompletionCapacityHttpReads(regId: String): HttpReads[Option[String]] =
    optionHttpReads("getCompletionCapacityHttpReads", Some(regId))

  def upsertCompletionCapacityHttpReads(regId: String): HttpReads[String] =
    httpReads("upsertCompletionCapacityHttpReads", Some(regId))

  def getAcknowledgementReferenceHttpReads(regId: String): HttpReads[Option[String]] =
    optionHttpReads("getAcknowledgementReferenceHttpReads", Some(regId))

  def getStatusHttpReads(regId: String): HttpReads[Option[PAYEStatus.Value]] = {
    implicit val rds = PAYEStatus.payeRegResponseReads
    optionHttpReads("getStatusHttpReads", Some(regId))
  }

  def deletionHttpReads(functionName: String, regId: String, txId: String): HttpReads[RegistrationDeletion.Value] = {
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK | NO_CONTENT | ACCEPTED => RegistrationDeletion.success
      case PRECONDITION_FAILED =>
        logger.warn(s"[$functionName] Deleting document for regId $regId and txId $txId failed as document was not rejected")
        RegistrationDeletion.invalidStatus
      case NOT_FOUND =>
        logger.info(s"[$functionName] paye reg returned 404 when expecting to find one to delete for $regId : $txId ")
        RegistrationDeletion.notfound
      case status =>
        unexpectedStatusHandling()(functionName, url, status, Some(regId), Some(txId))
    }
  }
}

object PAYERegistrationHttpParsers extends PAYERegistrationHttpParsers with BaseConnector
