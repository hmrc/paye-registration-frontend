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

import utils.Logging
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

package object connectors extends Logging {
  def logResponse(e: Throwable, method: String, msg: String, regId: Option[String] = None): Throwable = {
    val optRegId = regId.map(r => s" and regId: $regId").getOrElse("")

    def log(s: String) = logger.error(s"[$method] received $s when $msg$optRegId")

    e match {
      case _: NotFoundException => log("NOT FOUND")
      case _: BadRequestException => log("BAD REQUEST")
      case e: UpstreamErrorResponse => e.statusCode match {
        case status if status >= 500 => log(s"Upstream 5xx: $status")
        case 403 => log("FORBIDDEN")
        case _ => log(s"Upstream 4xx: ${e.statusCode} ${e.message}")
      }
      case e: Exception => log(s"ERROR: ${e.getMessage}")
    }
    e
  }
}
