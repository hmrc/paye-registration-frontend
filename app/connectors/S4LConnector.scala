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

import com.codahale.metrics.{Counter, Timer}
import config.PAYEShortLivedCache
import play.api.libs.json.Format
import play.api.mvc.Request
import services.MetricsService
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class S4LConnector @Inject()(val shortCache: PAYEShortLivedCache,
                                 val metricsService: MetricsService)(implicit val ec: ExecutionContext) {
  val successCounter: Counter = metricsService.s4lSuccessResponseCounter
  val emptyResponseCounter: Counter = metricsService.s4lEmptyResponseCounter
  val failedCounter: Counter = metricsService.s4lFailedResponseCounter

  def timer: Timer.Context = metricsService.s4lResponseTimer.time()

  def saveForm[T](userId: String, formId: String, data: T)(implicit hc: HeaderCarrier, format: Format[T], request: Request[_]): Future[CacheMap] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      shortCache.cache[T](userId, formId, data)
    }
  }

  def fetchAndGet[T](userId: String, formId: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, emptyResponseCounter, timer) {
      shortCache.fetchAndGetEntry[T](userId, formId)
    }
  }

  def clear(userId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] = {
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
