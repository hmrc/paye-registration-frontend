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
}

trait S4LConnect {

  val shortCache : ShortLivedCache
  val metricsService: MetricsSrv

  def saveForm[T](userId: String, formId: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    val s4lTimer = metricsService.s4lResponseTimer.time()
    shortCache.cache[T](userId, formId, data) map { saved =>
      s4lTimer.stop()
      saved
    }
  }

  def fetchAndGet[T](userId: String, formId: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    val s4lTimer = metricsService.s4lResponseTimer.time()
    shortCache.fetchAndGetEntry[T](userId, formId) map { fG =>
      s4lTimer.stop()
      fG
    }
  }

  def clear(userId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val s4lTimer = metricsService.s4lResponseTimer.time()
    shortCache.remove(userId) map { cleared =>
      s4lTimer.stop()
      cleared
    }
  }

  def fetchAll(userId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    val s4lTimer = metricsService.s4lResponseTimer.time()
    shortCache.fetch(userId) map { fetched =>
      s4lTimer.stop()
      fetched
    }
  }
}
