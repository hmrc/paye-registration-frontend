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

import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Request
import play.api.{LoggerLike, MarkerContext}
import uk.gov.hmrc.http.{HeaderNames, HttpResponse, SessionKeys}

trait Logging {

  private lazy val packageName: String =  this.getClass.getPackage.getName
  private lazy val className: String = this.getClass.getSimpleName.stripSuffix("$")

  lazy val trueClientIp: Request[_] => Option[String] = request => request.headers.get(HeaderNames.trueClientIp).map(trueClientIp => s"trueClientIp: $trueClientIp ")

  lazy val sessionId: Request[_] => Option[String] = request => request.session.get(SessionKeys.sessionId).map(sessionId => s"sessionId: $sessionId ")

  lazy val identifiers: Request[_] => String = request => Seq(trueClientIp(request), sessionId(request)).flatten.foldLeft("")(_ + _)

  lazy val trueClientIpFromHttpResponse: HttpResponse => Option[String] = httpResponse => httpResponse.headers.get(HeaderNames.trueClientIp).map(trueClientIp => s"trueClientIp: $trueClientIp")
  lazy val sessionIdFromHttpResponse: HttpResponse => Option[String] = httpResponse => httpResponse.headers.get(HeaderNames.xSessionId).map(sessionId => s"sessionId: $sessionId")
  lazy val identifiersFromHttpResponse: HttpResponse => String = request => Seq(trueClientIpFromHttpResponse(request), sessionIdFromHttpResponse(request)).flatten.foldLeft("")(_ + _)

  private lazy val prefixLog: String => String = msg =>
    s"[$className]${if (msg.startsWith("[")) msg else " " + msg}"

  val logger: LoggerLike = new LoggerLike {

    override val logger: Logger = LoggerFactory.getLogger(s"application.$packageName.$className")

    override def debug(message: => String)(implicit mc: MarkerContext): Unit = super.debug(prefixLog(message))
    override def info(message: => String)(implicit mc: MarkerContext): Unit = super.info(prefixLog(message))
    override def warn(message: => String)(implicit mc: MarkerContext): Unit = super.warn(prefixLog(message))
    override def error(message: => String)(implicit mc: MarkerContext): Unit = super.error(prefixLog(message))

    override def debug(message: => String, e: => Throwable)(implicit mc: MarkerContext): Unit = super.debug(prefixLog(message), e)
    override def info(message: => String, e: => Throwable)(implicit mc: MarkerContext): Unit = super.info(prefixLog(message), e)
    override def warn(message: => String, e: => Throwable)(implicit mc: MarkerContext): Unit = super.warn(prefixLog(message), e)
    override def error(message: => String, e: => Throwable)(implicit mc: MarkerContext): Unit = super.error(prefixLog(message), e)
  }

  def infoLog(message: => String)(implicit mc: MarkerContext, request: Request[_]): Unit = logger.info(s"$message (${identifiers(request)})")
  def warnLog(message: => String)(implicit mc: MarkerContext, request: Request[_]): Unit = logger.warn(s"$message (${identifiers(request)})")
  def errorLog(message: => String)(implicit mc: MarkerContext, request: Request[_]): Unit = logger.error(s"$message (${identifiers(request)})")

}