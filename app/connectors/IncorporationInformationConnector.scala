/*
 * Copyright 2018 HM Revenue & Customs
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

import com.codahale.metrics.{Counter, Timer}
import common.exceptions.DownstreamExceptions.OfficerListNotFoundException
import config.WSHttp
import models.external.{CoHoCompanyDetailsModel, OfficerList}
import play.api.Logger
import play.api.libs.json._
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.http.{BadRequestException, CoreGet, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import utils.RegistrationWhitelist

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

@Singleton
class IncorporationInformationConnector @Inject()(val metricsService: MetricsService) extends IncorporationInformationConnect with ServicesConfig {
  lazy val incorpInfoUrl = baseUrl("incorporation-information")
  lazy val incorpInfoUri = getConfString("incorporation-information.uri","")
  val http               = WSHttp
  val successCounter     = metricsService.companyDetailsSuccessResponseCounter
  val failedCounter      = metricsService.companyDetailsFailedResponseCounter
  def timer              = metricsService.incorpInfoResponseTimer.time()
}

sealed trait IncorpInfoResponse
case class IncorpInfoSuccessResponse(response: CoHoCompanyDetailsModel) extends IncorpInfoResponse
case object IncorpInfoBadRequestResponse extends IncorpInfoResponse
case class IncorpInfoErrorResponse(ex: Exception) extends IncorpInfoResponse

trait IncorporationInformationConnect extends RegistrationWhitelist {
  val incorpInfoUrl: String
  val incorpInfoUri: String
  val http: CoreGet
  val metricsService: MetricsSrv

  val successCounter: Counter
  val failedCounter: Counter
  def timer: Timer.Context

  def getCoHoCompanyDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[IncorpInfoResponse] = {
    ifRegIdNotWhitelisted(regId) {
      implicit val rds = CoHoCompanyDetailsModel.incorpInfoReads
      val incorpInfoTimer = timer
      http.GET[CoHoCompanyDetailsModel](s"$incorpInfoUrl$incorpInfoUri/$transactionId/company-profile") map { res =>
        incorpInfoTimer.stop()
        successCounter.inc(1)
        IncorpInfoSuccessResponse(res)
      } recover {
        case _: BadRequestException =>
          Logger.error(s"[IncorporationInformationConnector] [getCoHoCompanyDetails] - Received a BadRequest status code when expecting company details for regId: $regId / TX-ID: $transactionId")
          incorpInfoTimer.stop()
          failedCounter.inc(1)
          IncorpInfoBadRequestResponse
        case ex: Exception =>
          Logger.error(
            s"[IncorporationInformationConnector] [getCoHoCompanyDetails] - Received an error when expecting company details for regId: $regId / TX-ID: $transactionId - error: ${ex.getMessage}"
          )
          incorpInfoTimer.stop()
          failedCounter.inc(1)
          IncorpInfoErrorResponse(ex)
      }
    }
  }

  def getOfficerList(transactionId: String, regId:String)(implicit hc : HeaderCarrier): Future[OfficerList] = {
    ifRegIdNotWhitelisted(regId) {
      val incorpInfoTimer = metricsService.incorpInfoResponseTimer.time()
      http.GET[JsObject](s"$incorpInfoUrl$incorpInfoUri/$transactionId/officer-list") map { obj =>
        incorpInfoTimer.stop()
        val list = obj.\("officers").as[OfficerList]
        if (list.items.isEmpty) {
          Logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received an empty Officer list for TX-ID $transactionId")
          throw new OfficerListNotFoundException
        }
        else {
          list
        }
      } recover {
        case _: NotFoundException =>
          Logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received a NotFound status code when expecting an Officer list for TX-ID $transactionId")
          incorpInfoTimer.stop()
          throw new OfficerListNotFoundException
        case badRequestErr: BadRequestException =>
          Logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received a BadRequest status code when expecting an Officer list for TX-ID $transactionId")
          incorpInfoTimer.stop()
          throw badRequestErr
        case ex: Exception =>
          Logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received an error response when expecting an Officer list for TX-ID $transactionId - error: ${ex.getMessage}")
          incorpInfoTimer.stop()
          throw ex
      }
    }
  }
}
