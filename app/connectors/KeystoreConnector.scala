/*
 * Copyright 2019 HM Revenue & Customs
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
import enums.IncorporationStatus
import javax.inject.Inject
import models.api.SessionMap
import models.external.CurrentProfile
import play.api.libs.json.{Format, Json}
import repositories.SessionRepository
import services.MetricsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

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

  def cache[T](formId: String, regId: String, txId: String, body : T)(implicit hc: HeaderCarrier, format: Format[T]): Future[SessionMap] = {
    metricsService.processDataResponseWithMetrics[SessionMap](successCounter, failedCounter, timer) {
      val updatedCacheMap = SessionMap(sessionID, regId, txId, Map(formId -> Json.toJson(body)))
      sessionRepository().upsertSessionMap(updatedCacheMap) map(_ => updatedCacheMap)
    }
  }

  def cacheSessionMap(map: SessionMap)(implicit hc: HeaderCarrier): Future[SessionMap] = {
    metricsService.processDataResponseWithMetrics[SessionMap](successCounter, failedCounter, timer) {
      sessionRepository().upsertSessionMap(map) map(_ => map)
    }
  }

  def fetch()(implicit hc : HeaderCarrier) : Future[Option[SessionMap]] = {
    metricsService.processOptionalDataWithMetrics[SessionMap](successCounter, emptyResponseCounter, timer) {
      sessionRepository().getSessionMap(sessionID)
    }
  }

  def fetchAndGetFromKeystore(key: String)(implicit hc: HeaderCarrier, format: Format[CurrentProfile]): Future[Option[CurrentProfile]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, emptyResponseCounter, timer) {
      sessionCache.fetchAndGetEntry(key) flatMap { data =>
        data.fold(Future.successful(data)) { cp =>
          cache(key, cp.registrationID, cp.companyTaxRegistration.transactionId, cp).map(_ => data)
        }
      }
    }
  }


  def fetchAndGet[T](key : String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    metricsService.processOptionalDataWithMetrics[T](successCounter, emptyResponseCounter, timer) {
      sessionRepository().getSessionMap(sessionID).map {
        _.flatMap(_.getEntry(key))
      }
    }
  }

  def fetchByTransactionId(txId: String)(implicit hc: HeaderCarrier): Future[Option[SessionMap]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, emptyResponseCounter, timer) {
      sessionRepository().getLatestSessionMapByTransactionId(txId)
    }
  }

  def remove()(implicit hc : HeaderCarrier) : Future[Boolean] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      sessionRepository().removeDocument(sessionID)
    }
  }
}
