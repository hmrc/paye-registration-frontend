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

import javax.inject.{Inject, Singleton}

import config.WSHttp
import enums.DownstreamOutcome
import models.api.{CompanyDetails => CompanyDetailsAPI, Employment => EmploymentAPI, PAYERegistration => PAYERegistrationAPI}
import models.view.Address
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PAYERegistrationConnector @Inject()() extends PAYERegistrationConnect with ServicesConfig {
  val payeRegUrl = baseUrl("paye-registration")
  val http : WSHttp = WSHttp
}

trait PAYERegistrationConnect {

  val payeRegUrl: String
  val http: WSHttp

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
      case e: Exception => throw logResponse(e, "getCompanyDetails", "getting company details")
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
    def log(s: String) = Logger.warn(s"[PAYERegistrationConnector] [$f] received $s when $m")
    e match {
      case e: NotFoundException => log("NOT FOUND")
      case e: BadRequestException => log("BAD REQUEST")
      case e: Upstream4xxResponse => e.upstreamResponseCode match {
        case 403 => log("FORBIDDEN")
        case _ => log(s"Upstream 4xx: ${e.upstreamResponseCode} ${e.message}")
      }
      case e: Upstream5xxResponse => log(s"Upstream 5xx: ${e.upstreamResponseCode}")
      case e: Exception => log(s"ERROR: ${e.getMessage}")
    }
    e
  }
}
