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

package connectors

import config.WSHttp
import enums.DownstreamOutcome
import models.dataModels.PAYERegistration
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait PAYERegistrationResponse
case class PAYERegistrationSuccessResponse(response: PAYERegistration) extends PAYERegistrationResponse
case object PAYERegistrationNotFoundResponse extends PAYERegistrationResponse
case object PAYERegistrationBadRequestResponse extends PAYERegistrationResponse
case object PAYERegistrationForbiddenResponse extends PAYERegistrationResponse
case class PAYERegistrationErrorResponse(err: Exception) extends PAYERegistrationResponse

object PAYERegistrationConnector extends PAYERegistrationConnector with ServicesConfig {
  //$COVERAGE-OFF$
  val payeRegUrl = baseUrl("paye-registration")
  val http = WSHttp
  //$COVERAGE-ON$
}

trait PAYERegistrationConnector {

  val payeRegUrl: String
  val http: HttpGet with HttpPost with HttpPatch

  def createNewRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistration]): Future[PAYERegistrationResponse] = {
    http.PATCH[String, PAYERegistration](s"$payeRegUrl/register-for-paye/$regID/new", regID) map {
      reg => PAYERegistrationSuccessResponse(reg)
    } recover {
      case e: BadRequestException =>
        Logger.warn("[PAYERegistrationConnector] [createNewRegistration] received Bad Request response creating new PAYE Registration in backend")
        PAYERegistrationBadRequestResponse
      case e: ForbiddenException =>
        Logger.warn("[PAYERegistrationConnector] [createNewRegistration] received Forbidden response when creating new PAYE Registration in backend")
        PAYERegistrationForbiddenResponse
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [createNewRegistration] received error when creating new PAYE Registration in backend - Error: ${e.getMessage}")
        PAYERegistrationErrorResponse(e)
    }
  }

  def getCurrentRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistration]): Future[PAYERegistrationResponse] = {
    http.GET[PAYERegistration](s"$payeRegUrl/register-for-paye/$regID") map {
      reg => PAYERegistrationSuccessResponse(reg)
    } recover {
      case e: NotFoundException =>
        Logger.info("[PAYERegistrationConnector] [getCurrentRegistration] received Not Found response getting PAYE Registration from backend")
        PAYERegistrationNotFoundResponse
      case e: ForbiddenException =>
        Logger.warn("[PAYERegistrationConnector] [getCurrentRegistration] received Forbidden response when expecting PAYE Registration from backend")
        PAYERegistrationForbiddenResponse
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [getCurrentRegistration] received error when expecting current profile from backend - Error: ${e.getMessage}")
        PAYERegistrationErrorResponse(e)
    }
  }


  def addTestRegistration(reg: PAYERegistration)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistration]): Future[PAYERegistrationResponse] = {
    http.POST[PAYERegistration, PAYERegistration](s"$payeRegUrl/register-for-paye/test-only/insert-registration/${reg.registrationID}", reg) map {
      reg => PAYERegistrationSuccessResponse(reg)
    } recover {
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [getCurrentRegistration] received error when setting up test Registration - Error: ${e.getMessage}")
        PAYERegistrationErrorResponse(e)
    }
  }

  def testRegistrationTeardown()(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistration]): Future[DownstreamOutcome.Value] = {
    http.GET[HttpResponse](s"$payeRegUrl/register-for-paye/test-only/registration-teardown") map {
      resp => DownstreamOutcome.Success
    } recover {
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [testRegistrationTeardown] received error when clearing registration details - Error: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }

}
