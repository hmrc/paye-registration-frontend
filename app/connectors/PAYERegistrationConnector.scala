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
import models.api.{PAYERegistration => PAYERegistrationAPI}
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import models.api.{CompanyDetails => CompanyDetailsAPI}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait PAYERegistrationResponse
case class PAYERegistrationSuccessResponse[T](response: T) extends PAYERegistrationResponse
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

  def createNewRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[PAYERegistrationResponse] = {
    http.PATCH[String, PAYERegistrationAPI](s"$payeRegUrl/register-for-paye/$regID/new", regID) map {
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

  def getCompanyDetails(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[CompanyDetailsAPI]): Future[PAYERegistrationResponse] = {
    http.GET[CompanyDetailsAPI](s"$payeRegUrl/register-for-paye/$regID/company-details") map {
      details => PAYERegistrationSuccessResponse[Option[CompanyDetailsAPI]](Some(details))
    } recover {
      case e: NotFoundException =>
        PAYERegistrationSuccessResponse[Option[CompanyDetailsAPI]](None)
      case e: BadRequestException =>
        Logger.warn("[PAYERegistrationConnector] [getCompanyDetails] received Bad Request response when getting company details from microservice")
        PAYERegistrationBadRequestResponse
      case e: ForbiddenException =>
        Logger.warn("[PAYERegistrationConnector] [getCompanyDetails] received Forbidden response when getting company details from microservice")
        PAYERegistrationForbiddenResponse
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [getCompanyDetails] received error when getting company details from microservice - Error: ${e.getMessage}")
        PAYERegistrationErrorResponse(e)
    }
  }

  def addTestRegistration(reg: PAYERegistrationAPI)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[PAYERegistrationResponse] = {
    http.POST[PAYERegistrationAPI, PAYERegistrationAPI](s"$payeRegUrl/register-for-paye/test-only/insert-registration/${reg.registrationID}", reg) map {
      reg => PAYERegistrationSuccessResponse(reg)
    } recover {
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [getCurrentRegistration] received error when setting up test Registration - Error: ${e.getMessage}")
        PAYERegistrationErrorResponse(e)
    }
  }

  def testRegistrationTeardown()(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    http.GET[HttpResponse](s"$payeRegUrl/register-for-paye/test-only/registration-teardown") map {
      resp => DownstreamOutcome.Success
    } recover {
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [testRegistrationTeardown] received error when clearing registration details - Error: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }

}
