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
import javax.inject.Inject
import play.api.libs.json.Format
import services.MetricsService
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class S4LConnectorImpl @Inject()(val shortCache: ShortLivedCache,
                                 val metricsService: MetricsService) extends S4LConnector {
  val successCounter = metricsService.s4lSuccessResponseCounter
  val emptyResponseCounter = metricsService.s4lEmptyResponseCounter
  val failedCounter = metricsService.s4lFailedResponseCounter

  def timer = metricsService.s4lResponseTimer.time()
}

trait S4LConnector {
  val shortCache: ShortLivedCache
  val metricsService: MetricsService

  val successCounter: Counter
  val emptyResponseCounter: Counter
  val failedCounter: Counter

  def timer: Timer.Context

  def saveForm[T](userId: String, formId: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      shortCache.cache[T](userId, formId, data)
    }
  }

  def fetchAndGet[T](userId: String, formId: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, emptyResponseCounter, timer) {
      shortCache.fetchAndGetEntry[T](userId, formId)
    }
  }

  def clear(userId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      shortCache.remove(userId)
    }
  }

  def fetchAll(userId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, failedCounter, timer) {
      shortCache.fetch(userId)
    }
  }
}
