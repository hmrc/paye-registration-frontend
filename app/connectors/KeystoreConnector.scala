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
import config.PAYESessionCache
import play.api.libs.json.Format
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

@Singleton
class KeystoreConnector @Inject()(val sessionCache: PAYESessionCache, val metricsService: MetricsService) extends KeystoreConnect {
  val successCounter        = metricsService.keystoreSuccessResponseCounter
  val emptyResponseCounter  = metricsService.keystoreEmptyResponseCounter
  val failedCounter         = metricsService.keystoreFailedResponseCounter
  def timer                 = metricsService.keystoreResponseTimer.time()
}

trait KeystoreConnect {
  val sessionCache: SessionCache
  val metricsService: MetricsSrv

  val successCounter: Counter
  val emptyResponseCounter: Counter
  val failedCounter: Counter
  def timer: Timer.Context

  def cache[T](formId: String, body : T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    metricsService.processDataResponseWithMetrics[CacheMap](successCounter, failedCounter, timer) {
      sessionCache.cache[T](formId, body)
    }
  }

  def fetch()(implicit hc : HeaderCarrier) : Future[Option[CacheMap]] = {
    metricsService.processOptionalDataWithMetrics[CacheMap](successCounter, emptyResponseCounter, timer) {
      sessionCache.fetch()
    }
  }

  def fetchAndGet[T](key : String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    metricsService.processOptionalDataWithMetrics[T](successCounter, emptyResponseCounter, timer) {
      sessionCache.fetchAndGetEntry(key)
    }
  }

  def remove()(implicit hc : HeaderCarrier) : Future[HttpResponse] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      sessionCache.remove()
    }
  }
}
