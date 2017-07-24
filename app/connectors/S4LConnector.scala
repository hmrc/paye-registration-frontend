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

import com.codahale.metrics.{Counter, Timer}
import config.PAYEShortLivedCache
import play.api.libs.json.Format
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class S4LConnector @Inject()(payeShortLivedCache: PAYEShortLivedCache, injMetrics: MetricsService) extends S4LConnect {
  val shortCache : ShortLivedCache = payeShortLivedCache
  val metricsService = injMetrics
  val successCounter = metricsService.s4lSuccessResponseCounter
  val emptyResponseCounter = metricsService.s4lEmptyResponseCounter
  val failedCounter = metricsService.s4lFailedResponseCounter
  def timer = metricsService.s4lResponseTimer.time()
}

trait S4LConnect {

  val shortCache : ShortLivedCache
  val metricsService: MetricsSrv

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
    metricsService.processHttpResponseWithMetrics(successCounter, failedCounter, timer) {
      shortCache.remove(userId)
    }
  }

  def fetchAll(userId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, failedCounter, timer) {
      shortCache.fetch(userId)
    }
  }
}
