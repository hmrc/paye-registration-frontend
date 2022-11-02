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
import connectors.httpParsers.AddressLookupHttpParsers
import models.Address
import models.external._
import services.MetricsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

@Singleton
class AddressLookupConnector @Inject()(metricsService: MetricsService,
                                       http: HttpClient
                                      )(appConfig: AppConfig, implicit val ec: ExecutionContext) extends BaseConnector with AddressLookupHttpParsers {

  lazy val addressLookupFrontendUrl: String = appConfig.servicesConfig.baseUrl("address-lookup-frontend")

  def getAddress(id: String)(implicit hc: HeaderCarrier): Future[Address] =
    withTimer {
      http.GET[Address](s"$addressLookupFrontendUrl/api/confirmed?id=$id")(addressHttpReads, hc, ec)
    }

  def getOnRampUrl(alfJourneyConfig: AlfJourneyConfig)(implicit hc: HeaderCarrier): Future[String] =
    withTimer {
      http.POST[AlfJourneyConfig, String](s"$addressLookupFrontendUrl/api/v2/init", alfJourneyConfig)(AlfJourneyConfig.journeyConfigFormat, onRampHttpReads, hc, ec)
    }

  private def withTimer[T](f: => Future[T]) = {
    metricsService.processDataResponseWithMetrics(
      metricsService.addressLookupSuccessResponseCounter,
      metricsService.addressLookupFailedResponseCounter,
      metricsService.addressLookupResponseTimer.time()
    )(f)
  }
}

class ALFLocationHeaderNotSetException extends NoStackTrace
