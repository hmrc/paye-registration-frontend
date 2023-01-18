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
import connectors.BaseConnector
import play.api.http.Status.{CREATED, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Reads
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logging

import scala.util.{Failure, Success, Try}

trait BaseHttpReads extends Logging {
  _: BaseConnector =>

  def unexpectedStatusException(url: String, status: Int, regId: Option[String], txId: Option[String]): Exception =
    new Exception(s"Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")

  val rawReads: HttpReads[HttpResponse] = (_: String, _: String, response: HttpResponse) => response

  def httpReads[T](functionName: String,
                   regId: Option[String] = None,
                   txId: Option[String] = None,
                   defaultResponse: Option[T] = None)(implicit reads: Reads[T], mf: Manifest[T], request: Request[_]): HttpReads[T] = (_: String, url: String, response: HttpResponse) =>
    response.status match {
      case OK | CREATED =>
        jsonParse(response)(functionName, regId, txId)
      case status =>
        unexpectedStatusHandling(defaultResponse)(functionName, url, status, regId, txId)
    }

  def optionHttpReads[T](functionName: String,
                         regId: Option[String] = None,
                         txId: Option[String] = None,
                         logInfoMsg: Boolean = false,
                         defaultToNoneOnError: Boolean = false)(implicit reads: Reads[T], mf: Manifest[T], request: Request[_]): HttpReads[Option[T]] = (_: String, url: String, response: HttpResponse) =>
    response.status match {
      case OK | CREATED =>
        Try(jsonParse(response)(functionName, regId, txId)).toOption
      case status if is2xx(status) || status == NOT_FOUND =>
        if (logInfoMsg) infoLog(s"[$functionName] No data retrieved when calling url: '$url'" + logContext(regId, txId))
        None
      case status if defaultToNoneOnError =>
        unexpectedStatusHandling(Some(Option.empty[T]))(functionName, url, status, regId, txId)
      case status =>
        unexpectedStatusHandling()(functionName, url, status, regId, txId)
    }

  def seqHttpReads[T](functionName: String,
                      regId: Option[String] = None,
                      txId: Option[String] = None,
                      defaultToEmptyOnError: Boolean = false)(implicit reads: Reads[Seq[T]], mf: Manifest[T], request: Request[_]): HttpReads[Seq[T]] = (_: String, url: String, response: HttpResponse) =>
    response.status match {
      case OK | CREATED =>
        jsonParse(response)(functionName, regId, txId)
      case NOT_FOUND | NO_CONTENT =>
        Seq.empty
      case status =>
        if (defaultToEmptyOnError) {
          unexpectedStatusHandling(Some(Seq.empty[T]))(functionName, url, status, regId, txId)
        } else {
          unexpectedStatusHandling()(functionName, url, status, regId, txId)
        }
    }

  def basicUpsertReads[T](functionName: String,
                          upsert: T,
                          regId: Option[String] = None,
                          txId: Option[String] = None)(implicit reads: Reads[T], mf: Manifest[T], request: Request[_]): HttpReads[T] = (_: String, url: String, response: HttpResponse) =>
    response.status match {
      case OK | CREATED => upsert
      case status =>
        errorLog(s"[$functionName] Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")
        upsert
    }

  def jsonParse[T](response: HttpResponse)(functionName: String,
                                           regId: Option[String] = None,
                                           txId: Option[String] = None)(implicit reads: Reads[T], mf: Manifest[T], request: Request[_]): T =
    Try(response.json.as[T]) match {
      case Success(t) => t
      case Failure(e) =>
        errorLog(s"[$functionName] JSON returned could not be parsed to ${mf.runtimeClass.getTypeName} model${logContext(regId, txId)}")
        throw e
    }

  def unexpectedStatusHandling[T](defaultResult: => Option[T] = None)(functionName: String,
                                                                      url: String,
                                                                      status: Int,
                                                                      regId: Option[String] = None,
                                                                      transactionId: Option[String] = None)
                                 (implicit request: Request[_]): T = {
    errorLog(s"[$functionName] Calling url: '$url' returned unexpected status: '$status'${logContext(regId, transactionId)}")
    defaultResult.fold(throw unexpectedStatusException(url, status, regId, transactionId))(identity)
  }

  def unexpectedStatusConnectorHandling[T](defaultResult: => Option[T] = None)(functionName: String,
                                                                               url: String,
                                                                               status: Int,
                                                                               regId: Option[String] = None,
                                                                               transactionId: Option[String] = None)
                                          (implicit httpResponse: HttpResponse): T = {
    errorConnectorLog(s"[$functionName] Calling url: '$url' returned unexpected status: '$status'${logContext(regId, transactionId)}")
    defaultResult.fold(throw unexpectedStatusException(url, status, regId, transactionId))(identity)
  }

}
