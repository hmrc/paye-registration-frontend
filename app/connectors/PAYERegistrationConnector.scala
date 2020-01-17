/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.NoSuchElementException

import config.WSHttp
import enums.{DownstreamOutcome, PAYEStatus, RegistrationDeletion}
import javax.inject.Inject
import models.api.{Director, Employment, PAYEContact, SICCode, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.{Configuration, Environment, Logger}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Reads}
import services.MetricsService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

sealed trait DESResponse
object Success extends DESResponse
object Cancelled extends DESResponse
object Failed extends DESResponse
object TimedOut extends DESResponse

class PAYERegistrationConnectorImpl @Inject()(val metricsService: MetricsService,
                                              val http: WSHttp,
                                              override val runModeConfiguration: Configuration,
                                              environment: Environment) extends PAYERegistrationConnector with ServicesConfig {
  val payeRegUrl = baseUrl("paye-registration")
  override protected def mode = environment.mode
}

trait PAYERegistrationConnector {
  val payeRegUrl: String
  val http: CorePatch with CoreGet with CorePut with CoreDelete
  val metricsService: MetricsService

  def createNewRegistration(regID: String, txID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[DownstreamOutcome.Value] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[String, HttpResponse](s"$payeRegUrl/paye-registration/$regID/new", txID) map {
      _.status match {
        case OK =>
          payeRegTimer.stop()
          DownstreamOutcome.Success
      }
    } recover {
      case e: Exception =>
        logResponse(e, "createNewRegistration", "creating new registration", regID, Some(txID))
        payeRegTimer.stop()
        DownstreamOutcome.Failure
    }
  }

  def getRegistration(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYERegistrationAPI]): Future[PAYERegistrationAPI] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[PAYERegistrationAPI](s"$payeRegUrl/paye-registration/$regID") map { res =>
      payeRegTimer.stop()
      res
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getRegistration", "getting registration", regID)
    }
  }

  def getRegistrationId(txId: String)(implicit hc: HeaderCarrier): Future[String] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[String](s"$payeRegUrl/paye-registration/$txId/registration-id") map { res =>
      payeRegTimer.stop()
      res
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getRegistrationId by txId", "getting registrationId", "", Some(txId))
    }
  }

  def submitRegistration(regId: String)(implicit hc: HeaderCarrier): Future[DESResponse] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PUT[String, HttpResponse](s"$payeRegUrl/paye-registration/$regId/submit-registration", "") map {
      _.status match {
        case OK         =>
          payeRegTimer.stop()
          Success
        case NO_CONTENT =>
          payeRegTimer.stop()
          Cancelled
      }
    } recover {
      case e: Exception =>
        logResponse(e, "submitRegistration", "submitting PAYE Registration to DES", regId) match {
          case _ : Upstream5xxResponse  => TimedOut
          case _                        => Failed
        }
    }
  }

  def getCompanyDetails(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[CompanyDetailsAPI]): Future[Option[CompanyDetailsAPI]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details") map { details =>
      payeRegTimer.stop()
      Some(details)
    } recover {
      case _: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getCompanyDetails", "getting company details", regID)
    }
  }

  def upsertCompanyDetails(regID: String, companyDetails: CompanyDetailsAPI)(implicit hc: HeaderCarrier): Future[CompanyDetailsAPI] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[CompanyDetailsAPI, CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details", companyDetails) map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertCompanyDetails", "upserting company details", regID)
    }
  }

  def getEmployment(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[Employment]): Future[Option[Employment]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    val url = s"$payeRegUrl/paye-registration/$regID/employment-info"
    http.GET[HttpResponse](url) map { employment =>
      payeRegTimer.stop()
      if(employment.status == 204) None else employment.json.validate[Employment] fold(
        _   => throw new NoSuchElementException(s"Call to $url returned a ${employment.status} but no Employment could be created"),
        Some(_)
      )
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getEmployment", "getting employment", regID)
    }
  }

  def upsertEmployment (regID: String, employment: Employment)(implicit hc: HeaderCarrier): Future[Employment] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[Employment, Employment](s"$payeRegUrl/paye-registration/$regID/employment-info", employment) map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertEmployment", "upserting employment", regID)
    }
  }

  def getDirectors(regID: String)(implicit hc: HeaderCarrier): Future[Seq[Director]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors") map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case _: NotFoundException =>
        payeRegTimer.stop()
        Seq.empty
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getDirectors", "getting directors", regID)
    }
  }

  def upsertDirectors(regID: String, directors: Seq[Director])(implicit hc: HeaderCarrier, rds: HttpReads[Seq[Director]]) = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[Seq[Director], Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors", directors) map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertDirectors", "upserting directors", regID)
    }
  }

  def getSICCodes(regID: String)(implicit hc: HeaderCarrier): Future[Seq[SICCode]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes") map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case _: NotFoundException =>
        payeRegTimer.stop()
        Seq.empty
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getSICCodes", "getting sic codes", regID)
    }
  }

  def upsertSICCodes(regID: String, sicCodes: Seq[SICCode])(implicit hc: HeaderCarrier, rds: HttpReads[Seq[SICCode]]) = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[Seq[SICCode], Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes", sicCodes) map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertSICCodes", "upserting sic codes", regID)
    }
  }

  def getPAYEContact(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContact]): Future[Option[PAYEContact]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[PAYEContact](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye") map { details =>
      payeRegTimer.stop()
      Some(details)
    } recover {
      case _: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getPAYEContact", "getting paye contact", regID)
    }
  }

  def upsertPAYEContact(regID: String, payeContact: PAYEContact)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContact]): Future[PAYEContact] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[PAYEContact, PAYEContact](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye", payeContact) map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertPAYEContact", "upserting paye contact", regID)
    }
  }

  def getCompletionCapacity(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[Option[String]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[String](s"$payeRegUrl/paye-registration/$regID/capacity") map { details =>
      payeRegTimer.stop()
      Some(details)
    } recover {
      case _: NotFoundException =>
        payeRegTimer.stop()
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getCompletionCapacity", "getting completion capacity", regID)
    }
  }

  def upsertCompletionCapacity(regID: String, completionCapacity: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[String] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.PATCH[String, String](s"$payeRegUrl/paye-registration/$regID/capacity", completionCapacity) map { resp =>
      payeRegTimer.stop()
      resp
    } recover {
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "upsertCompletionCapacity", "upserting completion capacity", regID)
    }
  }

  def getAcknowledgementReference(regID: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[String](s"$payeRegUrl/paye-registration/$regID/acknowledgement-reference") map { ackRef =>
      payeRegTimer.stop()
      Some(ackRef)
    } recover {
      case e: NotFoundException =>
        payeRegTimer.stop()
        logResponse(e, "getAcknowledgementReference", "getting acknowledgement reference", regID)
        None
      case e: Exception =>
        payeRegTimer.stop()
        throw logResponse(e, "getAcknowledgementReference", "getting acknowledgement reference", regID)
    }
  }

  def getStatus(regId: String)(implicit hc: HeaderCarrier): Future[Option[PAYEStatus.Value]] = {
    val payeRegTimer = metricsService.payeRegistrationResponseTimer.time()
    http.GET[JsObject](s"$payeRegUrl/paye-registration/$regId/status") map { json =>
      payeRegTimer.stop()
      Some((json \ "status").as[PAYEStatus.Value](Reads.enumNameReads(PAYEStatus)))
    } recover {
      case _: NotFoundException =>
        payeRegTimer.stop()
        logger.info(s"[PAYERegistrationConnector] [getStatus] received NotFound when checking status for regId $regId")
        None
      case e : Throwable =>
        payeRegTimer.stop()
        logResponse(e, "getStatus", "getting PAYE registration document status", regId)
        None
    }
  }
// Test Endpoint
  def setBackendDate(date: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val newDate = if (date.isEmpty) "time-clear" else s"${date}Z"
    http.GET[HttpResponse](s"$payeRegUrl/paye-registration/test-only/feature-flag/system-date/$newDate") map {
      _ => true
    } recover {
      case _: Exception => false
    }
  }

  def deleteRejectedRegistrationDocument(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    http.DELETE[HttpResponse](s"$payeRegUrl/paye-registration/$regId/delete") map {
      _.status match {
        case OK => RegistrationDeletion.success
      }
    } recover {
      case fourXX: Upstream4xxResponse if fourXX.upstreamResponseCode == PRECONDITION_FAILED =>
        logger.warn(s"[PAYERegistrationConnector] - [deleteRejectedRegistrationDocument] Deleting document for regId $regId and txId $txId failed as document was not rejected")
        RegistrationDeletion.invalidStatus
      case fiveXX: Upstream5xxResponse =>
        throw logResponse(fiveXX, "deleteRejectedRegistrationDocument", s"deleting document, error message: ${fiveXX.message}", regId, Some(txId))
    }
  }

  def deleteCurrentRegistrationInProgress(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    http.DELETE[HttpResponse](s"$payeRegUrl/paye-registration/$regId/delete-in-progress") map {
      _.status match {
        case OK => RegistrationDeletion.success
      }
    } recover {
      case fourXX: Upstream4xxResponse if fourXX.upstreamResponseCode == PRECONDITION_FAILED =>
        logger.warn(s"[PAYERegistrationConnector] - [deleteCurrentRegistrationInProgress] Deleting document for regId $regId and txId $txId failed as document was not draft or invalid")
        RegistrationDeletion.invalidStatus
      case fiveXX: Upstream5xxResponse =>
        throw logResponse(fiveXX, "deleteCurrentRegistrationInProgress", s"deleting document, error message: ${fiveXX.message}", regId, Some(txId))
    }
  }

  def deleteRegistrationForRejectedIncorp(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    http.DELETE[HttpResponse](s"$payeRegUrl/paye-registration/$regId/delete-rejected-incorp") map {
      _.status match {
        case OK => RegistrationDeletion.success
      }
    } recover {
      case fourXX: Upstream4xxResponse if fourXX.upstreamResponseCode == PRECONDITION_FAILED =>
        logger.warn(s"[PAYERegistrationConnector] - [deleteRegistrationForRejectedIncorp] Deleting document for regId $regId and txId $txId failed as document was not draft or invalid")
        RegistrationDeletion.invalidStatus
      case notFound: Upstream4xxResponse if notFound.upstreamResponseCode == NOT_FOUND =>
        Logger.warn(s"s[PAYERegistrationConnector] - deleteRegistrationForRejectedIncorp paye reg returned 404 when expecting to find one for $regId : $txId ")
        RegistrationDeletion.notfound
      case fiveXX: Upstream5xxResponse =>
        throw logResponse(fiveXX, "deleteRegistrationForRejectedIncorp", s"deleting document, error message: ${fiveXX.message}", regId, Some(txId))
    }
  }

  private[connectors] def logResponse(e: Throwable, f: String, m: String, regId: String, txId: Option[String] = None): Throwable = {
    val optTxId = txId.map(t => s" and txId: $t").getOrElse("")
    def log(s: String) = logger.warn(s"[PAYERegistrationConnector] [$f] received $s when $m for regId: $regId$optTxId")
    e match {
      case e: NotFoundException   => log("NOT FOUND")
      case e: BadRequestException => log("BAD REQUEST")
      case e: Upstream4xxResponse => e.upstreamResponseCode match {
        case 403  => log("FORBIDDEN")
        case _    => log(s"Upstream 4xx: ${e.upstreamResponseCode} ${e.message}")
      }
      case e: Upstream5xxResponse => log(s"Upstream 5xx: ${e.upstreamResponseCode}")
      case e: Exception           => log(s"ERROR: ${e.getMessage}")
    }
    e
  }
}