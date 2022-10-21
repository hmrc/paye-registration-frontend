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

import common.exceptions
import common.exceptions.DownstreamExceptions
import connectors.{IncorpInfoBadRequestResponse, IncorpInfoNotFoundResponse, IncorpInfoResponse, IncorpInfoSuccessResponse}
import enums.{DownstreamOutcome, IncorporationStatus}
import models.external.{CoHoCompanyDetailsModel, IncorpUpdateResponse, OfficerList}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpReads, HttpResponse}
import utils.Logging

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait PAYERegistrationHttpParsers extends Logging with HttpErrorFunctions {

  def createNewRegistrationHttpReads(regId: String, transactionId: String): HttpReads[DownstreamOutcome.Value] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => DownstreamOutcome.Success
      case status =>
        unexpectedStatusHandling("createNewRegistrationHttpReads", regId, transactionId, url, status, Some(DownstreamOutcome.Failure))
    }

  private def unexpectedStatusHandling[T](functionName: String, regId: String, transactionId: String, url: String, status: Int, defaultResult: => Option[T] = None): T = {
    logger.error(s"[$functionName] An unexpected response was received when calling paye-registration for regId: $regId and txId: $transactionId. Status: '$status'")
    defaultResult.fold(throw new exceptions.DownstreamExceptions.PAYEMicroserviceException(s"Calling paye-registration on $url returned status: '$status'"))(identity)
  }
}


