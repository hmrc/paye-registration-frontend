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

import com.codahale.metrics.{Counter, Timer}
import common.Logging
import config.{FrontendAppConfig, WSHttp}
import javax.inject.{Inject, Singleton}
import models.Address
import models.external._
import play.api.i18n.MessagesApi
import play.api.libs.json.Reads
import services.MetricsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

@Singleton
class AddressLookupConnector @Inject()(metricsService: MetricsService,
                                       http: WSHttp,
                                       frontendAppConfig: FrontendAppConfig
                                      )(implicit messagesApi: MessagesApi)
  extends Logging {

  lazy val addressLookupFrontendUrl: String = frontendAppConfig.baseUrl("address-lookup-frontend")
  val successCounter: Counter = metricsService.addressLookupSuccessResponseCounter
  val failedCounter: Counter = metricsService.addressLookupFailedResponseCounter

  def timer: Timer.Context = metricsService.addressLookupResponseTimer.time()

  def getAddress(id: String)(implicit hc: HeaderCarrier): Future[Address] = {
    implicit val reads: Reads[Address] = Address.addressLookupReads
    metricsService.processDataResponseWithMetrics[Address](successCounter, failedCounter, timer) {
      http.GET[Address](s"$addressLookupFrontendUrl/api/confirmed?id=$id")
    }
  }

  def getOnRampUrl(alfJourneyConfig: AlfJourneyConfig)(implicit hc: HeaderCarrier): Future[String] = {
    val postUrl = s"$addressLookupFrontendUrl/api/init"

    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      http.POST[AlfJourneyConfig, HttpResponse](postUrl, alfJourneyConfig)
    } map {
      _.header("Location").getOrElse {
        logger.warn("[AddressLookupConnector] [getOnRampUrl] - ERROR: Location header not set in ALF response")
        throw new ALFLocationHeaderNotSetException
      }
    }
  }
}

class ALFLocationHeaderNotSetException extends NoStackTrace
