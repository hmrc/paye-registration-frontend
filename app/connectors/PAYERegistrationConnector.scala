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
import models.api.{Employment => EmploymentAPI, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

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

  def createNewRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[DownstreamOutcome.Value] = {
    http.PATCH[String, HttpResponse](s"$payeRegUrl/paye-registration/$regID/new", regID) map {
      response => response.status match {
        case Status.OK => DownstreamOutcome.Success
      }
    } recover {
      case e: Exception => logResponse(e, "createNewRegistration", "creating new registration")
        DownstreamOutcome.Failure
    }
  }

  def getRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[PAYERegistrationAPI] = {
    http.GET[PAYERegistrationAPI](s"$payeRegUrl/paye-registration/$regID") recover {
      case e: Exception => throw logResponse(e, "getRegistration", "getting registration")
    }
  }

  def getCompanyDetails(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[CompanyDetailsAPI]): Future[Option[CompanyDetailsAPI]] = {
    http.GET[CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details") map {
      details => Some(details)
    } recover {
      case e: NotFoundException => None
      case e: Exception => throw logResponse(e, "getEmployment", "getting employment")
    }
  }

  def upsertCompanyDetails(regID: String, companyDetails: CompanyDetailsAPI)(implicit hc: HeaderCarrier, rds: HttpReads[CompanyDetailsAPI]): Future[CompanyDetailsAPI] = {
    http.PATCH[CompanyDetailsAPI, CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details", companyDetails) recover {
      case e: Exception => throw logResponse(e, "upsertCompanyDetails", "upserting company details")
    }
  }

  def getEmployment(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[EmploymentAPI]): Future[Option[EmploymentAPI]] = {
    http.GET[EmploymentAPI](s"$payeRegUrl/paye-registration/$regID/employment") map {
      s => Some(s)
    } recover {
      case e: NotFoundException => None
      case e: Exception => throw logResponse(e, "getEmployment", "getting employment")
    }
  }

  def upsertEmployment(regID: String, employment: EmploymentAPI)(implicit hc: HeaderCarrier, rds: HttpReads[EmploymentAPI]): Future[EmploymentAPI] = {
    http.PATCH[EmploymentAPI, EmploymentAPI](s"$payeRegUrl/paye-registration/$regID/employment", employment) recover {
      case e: Exception => throw logResponse(e, "upsertEmployment", "upserting employment")
    }
  }

  private[connectors] def logResponse(e: Throwable, f: String, m: String): Throwable = {
    e match {
      case e: NotFoundException => Logger.warn(s"[PAYERegistrationConnector] [$f] received Not Found response when $m")
      case e: BadRequestException => Logger.warn(s"[PAYERegistrationConnector] [$f] received Bad Request response when $m")
      case e: Upstream4xxResponse => e.upstreamResponseCode match {
        case 403 => Logger.warn(s"[PAYERegistrationConnector] [$f] received Forbidden response when $m")
        case _ => Logger.warn(s"[PAYERegistrationConnector] [$f]" +
                              s" received Upstream 4xx response when $m containing ${e.upstreamResponseCode} ${e.message}")
      }
      case e: Upstream5xxResponse =>
        Logger.warn(s"[PAYERegistrationConnector] [$f]" +
                    s" received Upstream 5xx response when $m containing ${e.upstreamResponseCode}")
      case e: Exception => Logger.warn(s"[PAYERegistrationConnector] [$f]" +
        s" received error when $m - Error: ${e.getMessage}")
    }
    e
  }
}
