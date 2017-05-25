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
import enums.{DownstreamOutcome, PAYEStatus}
import models.api.{Director, Eligibility, PAYEContact, SICCode, CompanyDetails => CompanyDetailsAPI, Employment => EmploymentAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Reads}
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait DESResponse
object Success extends DESResponse
object Cancelled extends DESResponse
object Failed extends DESResponse
object TimedOut extends DESResponse

@Singleton
class PAYERegistrationConnector @Inject()(injMetrics: MetricsService) extends PAYERegistrationConnect with ServicesConfig {
  val payeRegUrl = baseUrl("paye-registration")
  val http : WSHttp = WSHttp
  val metricsService = injMetrics
}

trait PAYERegistrationConnect {

  val payeRegUrl: String
  val http: WSHttp
  val metricsService: MetricsSrv

  def createNewRegistration(regID: String, txID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[DownstreamOutcome.Value] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[String, HttpResponse](s"$payeRegUrl/paye-registration/$regID/new", txID) map {
      response => response.status match {
        case Status.OK =>
          payeRegTimer.stop()
          DownstreamOutcome.Success
      }
    } recover {
      case e: Exception => logResponse(e, "createNewRegistration", "creating new registration")
        payeRegTimer.stop()
        DownstreamOutcome.Failure
    }
  }

  def getRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[PAYERegistrationAPI] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[PAYERegistrationAPI](s"$payeRegUrl/paye-registration/$regID") map {
      res =>
        payeRegTimer.stop()
        res
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getRegistration", "getting registration")
    }
  }

  def submitRegistration(regId: String)(implicit hc: HeaderCarrier): Future[DESResponse] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PUT[String, HttpResponse](s"$payeRegUrl/paye-registration/$regId/submit-registration", "") map {
      response =>
        payeRegTimer.stop()
        response.status match {
          case Status.OK         => Success
          case Status.NO_CONTENT => Cancelled
        }
    } recover {
      case e: Exception =>
        logResponse(e, "submitRegistration", "submitting PAYE Registration to DES")
        e match {
          case _ : Upstream5xxResponse => TimedOut
          case _ => Failed
        }
    }
  }

  def getCompanyDetails(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[CompanyDetailsAPI]): Future[Option[CompanyDetailsAPI]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details") map {
      details =>
        payeRegTimer.stop()
        Some(details)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getCompanyDetails", "getting company details")
    }
  }

  def upsertCompanyDetails(regID: String, companyDetails: CompanyDetailsAPI)(implicit hc: HeaderCarrier, rds: HttpReads[CompanyDetailsAPI]): Future[CompanyDetailsAPI] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[CompanyDetailsAPI, CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details", companyDetails) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertCompanyDetails", "upserting company details")
    }
  }

  def getEmployment(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[EmploymentAPI]): Future[Option[EmploymentAPI]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[EmploymentAPI](s"$payeRegUrl/paye-registration/$regID/employment") map {
      s =>
        payeRegTimer.stop()
        Some(s)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getEmployment", "getting employment")
    }
  }

  def upsertEmployment(regID: String, employment: EmploymentAPI)(implicit hc: HeaderCarrier, rds: HttpReads[EmploymentAPI]): Future[EmploymentAPI] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[EmploymentAPI, EmploymentAPI](s"$payeRegUrl/paye-registration/$regID/employment", employment) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertEmployment", "upserting employment")
    }
  }

  def getDirectors(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[EmploymentAPI]): Future[Seq[Director]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors") map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        Seq.empty
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getDirectors", "getting directors")
    }
  }

  def upsertDirectors(regID: String, directors: Seq[Director])(implicit hc: HeaderCarrier, rds: HttpReads[Seq[Director]]) = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[Seq[Director], Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors", directors) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertDirectors", "upserting directors")
    }
  }

  def getSICCodes(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[EmploymentAPI]): Future[Seq[SICCode]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes") map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        Seq.empty
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getSICCodes", "getting sic codes")
    }
  }

  def upsertSICCodes(regID: String, sicCodes: Seq[SICCode])(implicit hc: HeaderCarrier, rds: HttpReads[Seq[SICCode]]) = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[Seq[SICCode], Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes", sicCodes) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertSICCodes", "upserting sic codes")
    }
  }

  def getPAYEContact(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContact]): Future[Option[PAYEContact]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[PAYEContact](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye") map {
      details =>
        payeRegTimer.stop()
        Some(details)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getPAYEContact", "getting paye contact")
    }
  }

  def upsertPAYEContact(regID: String, payeContact: PAYEContact)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContact]): Future[PAYEContact] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[PAYEContact, PAYEContact](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye", payeContact) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertPAYEContact", "upserting paye contact")
    }
  }

  def getCompletionCapacity(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[Option[String]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[String](s"$payeRegUrl/paye-registration/$regID/capacity") map {
      details =>
        payeRegTimer.stop()
        Some(details)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getCompletionCapacity", "getting completion capacity")
    }
  }

  def upsertCompletionCapacity(regID: String, completionCapacity: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[String] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[String, String](s"$payeRegUrl/paye-registration/$regID/capacity", completionCapacity) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertCompletionCapacity", "upserting completion capacity")
    }
  }

  def getEligibility(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[Option[Eligibility]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[Eligibility](s"$payeRegUrl/paye-registration/$regID/eligibility") map {
      details =>
        payeRegTimer.stop()
        Some(details)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getEligibility", "getting eligibility")
    }
  }

  def upsertEligibility(regID: String, data: Eligibility)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[Eligibility] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[Eligibility, Eligibility](s"$payeRegUrl/paye-registration/$regID/eligibility", data) map {
      resp =>
        payeRegTimer.stop()
        resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertEligibility", "upserting eligibility")
    }
  }

  def getAcknowledgementReference(regID: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[String](s"$payeRegUrl/paye-registration/$regID/acknowledgement-reference") map {
      ackRef =>
        payeRegTimer.stop()
        Some(ackRef)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getAcknowledgementReference", "getting acknowledgement reference")
    }
  }

  def getStatus(regId: String)(implicit hc: HeaderCarrier): Future[Option[PAYEStatus.Value]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[JsObject](s"$payeRegUrl/paye-registration/$regId/status") map { json =>
      Some((json \ "status").as[PAYEStatus.Value](Reads.enumNameReads(PAYEStatus)))
    } recover {
      case e : Throwable =>
        logResponse(e, "getStatus", "getting PAYE registration document status")
        None
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
