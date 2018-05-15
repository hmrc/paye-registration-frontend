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

import javax.inject.Inject
import com.codahale.metrics.{Counter, Timer}
import play.api.libs.json.Format
import services.MetricsService
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import repositories.SessionRepository
import utils.CascadeUpsert

import scala.concurrent.Future

class KeystoreConnectorImpl @Inject()(val sessionCache: SessionCache,
                                      val metricsService: MetricsService,
                                      val sessionRepository: SessionRepository) extends KeystoreConnector {
  val successCounter        = metricsService.keystoreSuccessResponseCounter
  val emptyResponseCounter  = metricsService.keystoreEmptyResponseCounter
  val failedCounter         = metricsService.keystoreFailedResponseCounter
  def timer                 = metricsService.keystoreResponseTimer.time()
}

trait KeystoreConnector {
  val sessionCache: SessionCache
  val metricsService: MetricsService
  val sessionRepository: SessionRepository


  val successCounter: Counter
  val emptyResponseCounter: Counter
  val failedCounter: Counter
  def timer: Timer.Context

  private def sessionID(implicit hc: HeaderCarrier): String = hc.sessionId.getOrElse(throw new RuntimeException("Active User had no Session ID")).value

  def cache[T](formId: String, body : T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    metricsService.processDataResponseWithMetrics[CacheMap](successCounter, failedCounter, timer) {
      //sessionCache.cache[T](formId, body)
      sessionRepository().get(sessionID).flatMap { map =>
        val updatedCacheMap = CascadeUpsert(formId, body, map.getOrElse(new CacheMap(sessionID, Map())))
        sessionRepository().upsert(updatedCacheMap).map { _ => updatedCacheMap }
      }
    }
  }

  def fetch()(implicit hc : HeaderCarrier) : Future[Option[CacheMap]] = {
    metricsService.processOptionalDataWithMetrics[CacheMap](successCounter, emptyResponseCounter, timer) {
//      sessionCache.fetch()
      sessionRepository().get(sessionID)
    }
  }

  def fetchAndGet[T](key : String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    metricsService.processOptionalDataWithMetrics[T](successCounter, emptyResponseCounter, timer) {
//      sessionCache.fetchAndGetEntry(key)
      sessionRepository().get(sessionID).map{_.flatMap(_.getEntry(key))
      }
    }
  }

  def remove()(implicit hc : HeaderCarrier) : Future[Boolean] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
//      sessionCache.remove()
      sessionRepository().get(sessionID).flatMap { optionalCacheMap =>
        optionalCacheMap.fold(Future(false)) { _ =>
          sessionRepository().removeDocument(sessionID)
        }
      }
    }
  }
}
