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

package connectors

import config.AppConfig
import connectors.httpParsers.PAYERegistrationHttpParsers
import enums.{DownstreamOutcome, PAYEStatus, RegistrationDeletion}
import models.api.{Director, Employment, PAYEContact, SICCode, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.libs.json.Writes
import services.MetricsService
import uk.gov.hmrc.http._
import play.api.mvc.Request

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait DESResponse

object Success extends DESResponse

object Cancelled extends DESResponse

object Failed extends DESResponse

object TimedOut extends DESResponse

class PAYERegistrationConnector @Inject()(val metricsService: MetricsService,
                                          val http: HttpClient,
                                          appConfig: AppConfig)(implicit val ec: ExecutionContext) extends BaseConnector with PAYERegistrationHttpParsers {

  val payeRegUrl = appConfig.servicesConfig.baseUrl("paye-registration")

  def createNewRegistration(regID: String, txID: String)
                           (implicit hc: HeaderCarrier, request: Request[_]): Future[DownstreamOutcome.Value] = {
    infoLog("[createNewRegistration] attempting to create new registration")
    withTimer {
      withRecovery(Some(DownstreamOutcome.Failure))("createNewRegistration", Some(regID), Some(txID)) {
        http.PATCH[String, DownstreamOutcome.Value](s"$payeRegUrl/paye-registration/$regID/new", txID)(implicitly, createNewRegistrationHttpReads(regID, txID), hc, ec)
      }
    }
  }

  def getRegistration(regID: String)
                     (implicit hc: HeaderCarrier, request: Request[_]): Future[PAYERegistrationAPI] =
    withTimer {
      withRecovery()("getRegistration", Some(regID)) {
        http.GET[PAYERegistrationAPI](s"$payeRegUrl/paye-registration/$regID")(getRegistrationHttpReads(regID), hc, ec)
      }
    }

  def getRegistrationId(txId: String)
                       (implicit hc: HeaderCarrier, request: Request[_]): Future[String] =
    withTimer {
      withRecovery()("getRegistrationId", txId = Some(txId)) {
        http.GET[String](s"$payeRegUrl/paye-registration/$txId/registration-id")(getRegistrationIdHttpReads(txId), hc, ec)
      }
    }

  def submitRegistration(regId: String)
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[DESResponse] =
    withTimer {
      withRecovery[DESResponse](Some(Failed))("submitRegistration", Some(regId)) {
        http.PUT[String, DESResponse](s"$payeRegUrl/paye-registration/$regId/submit-registration", "")(implicitly, submitRegistrationHttpReads(regId), hc, ec)
      }
    }

  def getCompanyDetails(regID: String)
                       (implicit hc: HeaderCarrier, request: Request[_]): Future[Option[CompanyDetailsAPI]] =
    withTimer {
      withRecovery()("getCompanyDetails", Some(regID)) {
        http.GET[Option[CompanyDetailsAPI]](s"$payeRegUrl/paye-registration/$regID/company-details")(getCompanyDetailsHttpReads(regID), hc, ec)
      }
    }

  def upsertCompanyDetails(regID: String, companyDetails: CompanyDetailsAPI)
                          (implicit hc: HeaderCarrier, request: Request[_]): Future[CompanyDetailsAPI] =
    withTimer {
      withRecovery()("upsertCompanyDetails", Some(regID)) {
        http.PATCH[CompanyDetailsAPI, CompanyDetailsAPI](s"$payeRegUrl/paye-registration/$regID/company-details", companyDetails)(
          CompanyDetailsAPI.format, upsertCompanyDetailsHttpReads(regID), hc, ec
        )
      }
    }

  def getEmployment(regID: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[Employment]] =
    withTimer {
      withRecovery()("getEmployment", Some(regID)) {
        http.GET[Option[Employment]](s"$payeRegUrl/paye-registration/$regID/employment-info")(getEmploymentHttpReads(regID), hc, ec)
      }
    }

  def upsertEmployment(regID: String, employment: Employment)(implicit hc: HeaderCarrier, request: Request[_]): Future[Employment] =
    withTimer {
      withRecovery()("upsertEmployment", Some(regID)) {
        http.PATCH[Employment, Employment](s"$payeRegUrl/paye-registration/$regID/employment-info", employment)(Employment.format, upsertEmploymentHttpReads(regID), hc, ec)
      }
    }

  def getDirectors(regID: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[Director]] =
    withTimer {
      withRecovery()("getDirectors", Some(regID)) {
        http.GET[Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors")(directorsHttpReads(regID), hc, ec)
      }
    }

  def upsertDirectors(regID: String, directors: Seq[Director])(implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[Director]] =
    withTimer {
      withRecovery()("upsertDirectors", Some(regID)) {
        http.PATCH[Seq[Director], Seq[Director]](s"$payeRegUrl/paye-registration/$regID/directors", directors)(Writes.seq[Director], directorsHttpReads(regID), hc, ec)
      }
    }

  def getSICCodes(regID: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[SICCode]] =
    withTimer {
      withRecovery()("getSICCodes", Some(regID)) {
        http.GET[Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes")(sicCodesHttpReads(regID), hc, ec)
      }
    }

  def upsertSICCodes(regID: String, sicCodes: Seq[SICCode])(implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[SICCode]] =
    withTimer {
      withRecovery()("upsertSICCodes", Some(regID)) {
        http.PATCH[Seq[SICCode], Seq[SICCode]](s"$payeRegUrl/paye-registration/$regID/sic-codes", sicCodes)(Writes.seq[SICCode], sicCodesHttpReads(regID), hc, ec)
      }
    }

  def getPAYEContact(regID: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[PAYEContact]] =
    withTimer {
      withRecovery()("getPAYEContact", Some(regID)) {
        http.GET[Option[PAYEContact]](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye")(getPAYEContactHttpReads(regID), hc, ec)
      }
    }

  def upsertPAYEContact(regID: String, payeContact: PAYEContact)(implicit hc: HeaderCarrier, request: Request[_]): Future[PAYEContact] =
    withTimer {
      withRecovery()("upsertPAYEContact", Some(regID)) {
        http.PATCH[PAYEContact, PAYEContact](s"$payeRegUrl/paye-registration/$regID/contact-correspond-paye", payeContact)(PAYEContact.format, upsertPAYEContactHttpReads(regID), hc, ec)
      }
    }

  def getCompletionCapacity(regID: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] =
    withTimer {
      withRecovery()("getCompletionCapacity", Some(regID)) {
        http.GET[Option[String]](s"$payeRegUrl/paye-registration/$regID/capacity")(getCompletionCapacityHttpReads(regID), hc, ec)
      }
    }

  def upsertCompletionCapacity(regID: String, completionCapacity: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[String] =
    withTimer {
      withRecovery()("upsertCompletionCapacity", Some(regID)) {
        http.PATCH[String, String](s"$payeRegUrl/paye-registration/$regID/capacity", completionCapacity)(implicitly, upsertCompletionCapacityHttpReads(regID), hc, ec)
      }
    }

  def getAcknowledgementReference(regID: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] =
    withTimer {
      withRecovery()("getAcknowledgementReference", Some(regID)) {
        http.GET[Option[String]](s"$payeRegUrl/paye-registration/$regID/acknowledgement-reference")(getAcknowledgementReferenceHttpReads(regID), hc, ec)
      }
    }

  def getStatus(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[PAYEStatus.Value]] =
    withTimer {
      withRecovery[Option[PAYEStatus.Value]](None)("getStatus", Some(regId)) {
        http.GET[Option[PAYEStatus.Value]](s"$payeRegUrl/paye-registration/$regId/status")(getStatusHttpReads(regId), hc, ec)
      }
    }

  def deleteRejectedRegistrationDocument(regId: String, txId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[RegistrationDeletion.Value] =
    withRecovery()("deleteRejectedRegistrationDocument", Some(regId), Some(txId)) {
      http.DELETE[RegistrationDeletion.Value](s"$payeRegUrl/paye-registration/$regId/delete")(
        deletionHttpReads("deleteRejectedRegistrationDocument", regId, txId), hc, ec
      )
    }

  def deleteCurrentRegistrationInProgress(regId: String, txId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[RegistrationDeletion.Value] =
    withRecovery()("deleteCurrentRegistrationInProgress", Some(regId), Some(txId)) {
      http.DELETE[RegistrationDeletion.Value](s"$payeRegUrl/paye-registration/$regId/delete-in-progress")(
        deletionHttpReads("deleteCurrentRegistrationInProgress", regId, txId), hc, ec
      )
    }

  def deleteRegistrationForRejectedIncorp(regId: String, txId: String)
                                         (implicit hc: HeaderCarrier, request: Request[_]): Future[RegistrationDeletion.Value] =
    withRecovery()("deleteRegistrationForRejectedIncorp", Some(regId), Some(txId)) {
      http.DELETE[RegistrationDeletion.Value](s"$payeRegUrl/paye-registration/$regId/delete-rejected-incorp")(
        deletionHttpReads("deleteRegistrationForRejectedIncorp", regId, txId), hc, ec
      )
    }

  // Test Endpoint
  def setBackendDate(date: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val newDate = if (date.isEmpty) "time-clear" else s"${date}Z"
    http.GET[HttpResponse](s"$payeRegUrl/paye-registration/test-only/feature-flag/system-date/$newDate")(rawReads, hc, ec) map {
      _ => true
    } recover {
      case _: Exception => false
    }
  }

  private def withTimer[T](f: => Future[T]) =
    metricsService.processDataResponseWithMetrics1(metricsService.payeRegistrationResponseTimer.time())(f)
}