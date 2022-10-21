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

package connectors

import config.AppConfig
import connectors.httpParsers.PAYERegistrationHttpParsers
import enums.{DownstreamOutcome, PAYEStatus, RegistrationDeletion}
import models.api.{Director, Employment, PAYEContact, SICCode, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Reads}
import services.MetricsService
import uk.gov.hmrc.http.UpstreamErrorResponse.unapply
import uk.gov.hmrc.http._

import java.util.NoSuchElementException
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait DESResponse
object Success extends DESResponse
object Cancelled extends DESResponse
object Failed extends DESResponse
object TimedOut extends DESResponse

class PAYERegistrationConnector @Inject()(val metricsService: MetricsService,
                                          val http: HttpClient,
                                          appConfig: AppConfig)(implicit val ec: ExecutionContext) extends PAYERegistrationHttpParsers {

  val payeRegUrl = appConfig.servicesConfig.baseUrl("paye-registration")

  def createNewRegistration(regID: String, txID: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] =
    withTimer {
      withRecovery(Some(DownstreamOutcome.Failure))("createNewRegistration", Some(regID), Some(txID)) {
        http.PATCH[String, DownstreamOutcome.Value](s"$payeRegUrl/paye-registration/$regID/new", txID)(implicitly, createNewRegistrationHttpReads(regID, txID), hc, ec)
      }
    }

  def getRegistration(regID: String)(implicit hc: HeaderCarrier): Future[PAYERegistrationAPI] = withTimer {
    withRecovery()("getRegistration", Some(regID)) {
      http.GET[PAYERegistrationAPI](s"$payeRegUrl/paye-registration/$regID")
    }
  }

  def getRegistrationId(txId: String)(implicit hc: HeaderCarrier): Future[String] =
    withTimer {
      withRecovery()("getRegistration", txId = Some(txId)) {
        http.GET[String](s"$payeRegUrl/paye-registration/$txId/registration-id")
      }
    }

  def submitRegistration(regId: String)(implicit hc: HeaderCarrier): Future[DESResponse] =
    withTimer {
      withRecovery[DESResponse](Some(Failed))("submitRegistration", Some(regId)) {
        http.PUT[String, HttpResponse](s"$payeRegUrl/paye-registration/$regId/submit-registration", "") map {
          _.status match {
            case OK => Success
            case NO_CONTENT => Cancelled
          }
        } recover {
          case error: UpstreamErrorResponse if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined =>
            logger.error("[submitRegistration] Timed out when submitting PAYE Registration to DES")
            TimedOut
        }
      }
    }

  def getCompanyDetails(regID: String)(implicit hc: HeaderCarrier): Future[Option[CompanyDetailsAPI]] =
    withTimer {
      withRecovery()("getCompanyDetails", Some(regID)) {
        http.GET[Option[CompanyDetailsAPI]](s"$payeRegUrl/paye-registration/$regID/company-details")
      }
    }

  def upsertCompanyDetails(regID: String, companyDetails: CompanyDetailsAPI)(implicit hc: HeaderCarrier): Future[CompanyDetailsAPI] =
    withTimer {
      withRecovery()("upsertCompanyDetails", Some(regID)) {
        http.PATCH[CompanyDetailsAPI, CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details", companyDetails)
      }
    }

  def getEmployment(regID: String)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    withTimer {
      withRecovery()("getEmployment", Some(regID)) {
        val url = s"$payeRegUrl/paye-registration/$regID/employment-info"
        http.GET[HttpResponse](url) map { employment =>
          if (employment.status == 204) None else employment.json.validate[Employment] fold(
            _ => throw new NoSuchElementException(s"Call to $url returned a ${employment.status} but no Employment could be created"),
            Some(_)
          )
        }
      }
    }

  def upsertEmployment(regID: String, employment: Employment)(implicit hc: HeaderCarrier): Future[Employment] =
    withTimer {
      withRecovery()("upsertEmployment", Some(regID)) {
        http.PATCH[Employment, Employment](s"$payeRegUrl/paye-registration/$regID/employment-info", employment)
      }
    }

  def getDirectors(regID: String)(implicit hc: HeaderCarrier): Future[Seq[Director]] =
    withTimer {
      withRecovery()("getDirectors", Some(regID)) {
        http.GET[Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors") recover {
          case _: NotFoundException =>
            Seq.empty
        }
      }
    }

  def upsertDirectors(regID: String, directors: Seq[Director])(implicit hc: HeaderCarrier, rds: HttpReads[Seq[Director]]) =
    withTimer {
      withRecovery()("upsertDirectors", Some(regID)) {
        http.PATCH[Seq[Director], Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors", directors)
      }
    }

  def getSICCodes(regID: String)(implicit hc: HeaderCarrier): Future[Seq[SICCode]] =
    withTimer {
      withRecovery()("getSICCodes", Some(regID)) {
        http.GET[Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes") recover {
          case _: NotFoundException =>
            Seq.empty
        }
      }
    }

  def upsertSICCodes(regID: String, sicCodes: Seq[SICCode])(implicit hc: HeaderCarrier, rds: HttpReads[Seq[SICCode]]) =
    withTimer {
      withRecovery()("upsertSICCodes", Some(regID)) {
        http.PATCH[Seq[SICCode], Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes", sicCodes)
      }
    }

  def getPAYEContact(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContact]): Future[Option[PAYEContact]] =
    withTimer {
      withRecovery()("getPAYEContact", Some(regID)) {
        http.GET[Option[PAYEContact]](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye")
      }
    }

  def upsertPAYEContact(regID: String, payeContact: PAYEContact)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContact]): Future[PAYEContact] =
    withTimer {
      withRecovery()("upsertPAYEContact", Some(regID)) {
        http.PATCH[PAYEContact, PAYEContact](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye", payeContact)
      }
    }

  def getCompletionCapacity(regID: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[Option[String]] =
    withTimer {
      withRecovery()("getCompletionCapacity", Some(regID)) {
        http.GET[Option[String]](s"$payeRegUrl/paye-registration/$regID/capacity")
      }
    }

  def upsertCompletionCapacity(regID: String, completionCapacity: String)(implicit hc: HeaderCarrier, rds: HttpReads[String]): Future[String] =
    withTimer {
      withRecovery()("upsertCompletionCapacity", Some(regID)) {
        http.PATCH[String, String](s"$payeRegUrl/paye-registration/$regID/capacity", completionCapacity)
      }
    }

  def getAcknowledgementReference(regID: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    withTimer {
      withRecovery()("getAcknowledgementReference", Some(regID)) {
        http.GET[Option[String]](s"$payeRegUrl/paye-registration/$regID/acknowledgement-reference")
      }
    }

  def getStatus(regId: String)(implicit hc: HeaderCarrier): Future[Option[PAYEStatus.Value]] =
    withTimer {
      withRecovery[Option[PAYEStatus.Value]](None)("getAcknowledgementReference", Some(regId)) {
        http.GET[JsObject](s"$payeRegUrl/paye-registration/$regId/status") map { json =>
          Some((json \ "status").as[PAYEStatus.Value](Reads.enumNameReads(PAYEStatus)))
        } recover {
          case _: NotFoundException =>
            logger.info(s"[getStatus] received NotFound when checking status for regId $regId")
            None
        }
      }
    }

  def deleteRejectedRegistrationDocument(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] =
    withRecovery()("deleteRejectedRegistrationDocument", Some(regId), Some(txId)) {
      http.DELETE[HttpResponse](s"$payeRegUrl/paye-registration/$regId/delete") map {
        _.status match {
          case OK => RegistrationDeletion.success
        }
      } recover {
        case response: UpstreamErrorResponse if response.statusCode == PRECONDITION_FAILED =>
          logger.warn(s"[deleteRejectedRegistrationDocument] Deleting document for regId $regId and txId $txId failed as document was not rejected")
          RegistrationDeletion.invalidStatus
      }
    }

  def deleteCurrentRegistrationInProgress(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] =
    withRecovery()("deleteCurrentRegistrationInProgress", Some(regId), Some(txId)) {
      http.DELETE[HttpResponse](s"$payeRegUrl/paye-registration/$regId/delete-in-progress") map {
        _.status match {
          case OK => RegistrationDeletion.success
        }
      } recover {
        case response: UpstreamErrorResponse if response.statusCode == PRECONDITION_FAILED =>
          logger.warn(s"[deleteCurrentRegistrationInProgress] Deleting document for regId $regId and txId $txId failed as document was not draft or invalid")
          RegistrationDeletion.invalidStatus
      }
    }

  def deleteRegistrationForRejectedIncorp(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] =
    withRecovery()("deleteRegistrationForRejectedIncorp", Some(regId), Some(txId)) {
      http.DELETE[HttpResponse](s"$payeRegUrl/paye-registration/$regId/delete-rejected-incorp") map {
        _.status match {
          case OK => RegistrationDeletion.success
        }
      } recover {
        case response: UpstreamErrorResponse if response.statusCode == PRECONDITION_FAILED =>
          logger.warn(s"[deleteRegistrationForRejectedIncorp] Deleting document for regId $regId and txId $txId failed as document was not draft or invalid")
          RegistrationDeletion.invalidStatus
        case response: UpstreamErrorResponse if response.statusCode == NOT_FOUND =>
          logger.warn(s"[deleteRegistrationForRejectedIncorp] paye reg returned 404 when expecting to find one for $regId : $txId ")
          RegistrationDeletion.notfound
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

  private def withTimer[T](f: => Future[T]) =
    metricsService.processDataResponseWithMetrics(metricsService.payeRegistrationResponseTimer.time())(f)

  private def withRecovery[T](response: => Option[T] = None)(functionName: String, regId: Option[String] = None, txId: Option[String] = None)(f: => Future[T]) =
    f recover { case ex: Exception =>
      logger.error(s"[$functionName] Exception of type '${ex.getClass.getSimpleName}' was thrown${regId.fold("")(" for regId: " + _)}${txId.fold("")(" for txId: " + _)}")
      response.fold(throw ex)(identity)
    }
}