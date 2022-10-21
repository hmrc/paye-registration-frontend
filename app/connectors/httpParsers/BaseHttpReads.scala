/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Reads
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait BaseHttpReads extends Logging {

  def unexpectedStatusException(url: String, status: Int, regId: Option[String], txId: Option[String]): Exception

  def httpReads[T](functionName: String, regId: Option[String] = None, txId: Option[String] = None)(implicit reads: Reads[T], mf: Manifest[T]): HttpReads[T] =
    (_: String, url: String, response: HttpResponse) =>
      response.status match {
        case OK => jsonParse(response)(functionName, regId, txId)
        case status =>
          unexpectedStatusHandling()(functionName, url, status, regId, txId)
      }

  def optionHttpReads[T](functionName: String, regId: Option[String] = None, txId: Option[String] = None)(implicit reads: Reads[T], mf: Manifest[T]): HttpReads[Option[T]] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => Some(jsonParse(response)(functionName, regId, txId))
      case NOT_FOUND | NO_CONTENT => None
      case status =>
        unexpectedStatusHandling()(functionName, url, status, regId, txId)
    }

  def seqHttpReads[T](functionName: String, regId: Option[String] = None, txId: Option[String] = None)(implicit reads: Reads[T], mf: Manifest[T]): HttpReads[Seq[T]] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => jsonParse[Seq[T]](response)(functionName, regId, txId)
      case NOT_FOUND | NO_CONTENT => Seq.empty
      case status =>
        unexpectedStatusHandling()(functionName, url, status, regId, txId)
    }

  def jsonParse[T](response: HttpResponse)(functionName: String, regId: Option[String] = None, txId: Option[String] = None)(implicit reads: Reads[T], mf: Manifest[T]): T =
    Try(response.json.as[T]) match {
      case Success(t) => t
      case Failure(e) =>
        logger.error(s"[$functionName] JSON returned could not be parsed to ${mf.runtimeClass.getTypeName} model${logContext(regId, txId)}")
        throw e
    }

  def unexpectedStatusHandling[T](defaultResult: => Option[T] = None)(functionName: String, url: String, status: Int, regId: Option[String] = None, transactionId: Option[String] = None): T = {
    logger.error(s"[$functionName] Calling url: '$url' returned unexpected status: '$status'${logContext(regId, transactionId)}")
    defaultResult.fold(throw unexpectedStatusException(url, status, regId, transactionId))(identity)
  }

  def withRecovery[T](response: => Option[T] = None)(functionName: String, regId: Option[String] = None, txId: Option[String] = None)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] =
    f recover { case ex: Exception =>
      logger.error(s"[$functionName] Exception of type '${ex.getClass.getSimpleName}' was thrown${logContext(regId, txId)}")
      response.fold(throw ex)(identity)
    }

  def logContext(regId: Option[String] = None, txId: Option[String] = None): String = (regId, txId) match {
    case (Some(rId), Some(tId)) => s" for regId: '$rId' and txId: '$tId'"
    case (Some(rId), None) => s" for regId: '$rId'"
    case (None, Some(tId)) => s" for txId: '$tId'"
    case _ => ""
  }

}
