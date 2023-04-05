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
import config.PAYESessionCache
import models.api.SessionMap
import models.external.CurrentProfile
import play.api.libs.json.{Format, Json}
import play.api.mvc.Request
import repositories.SessionRepository
import services.MetricsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

class KeystoreConnector @Inject()(val sessionCache: PAYESessionCache,
                                  val metricsService: MetricsService,
                                  val sessionRepository: SessionRepository)(implicit val ec: ExecutionContext) extends Logging {
  val successCounter: Counter = metricsService.keystoreSuccessResponseCounter
  val emptyResponseCounter: Counter = metricsService.keystoreEmptyResponseCounter
  val failedCounter: Counter = metricsService.keystoreFailedResponseCounter

  def timer: Timer.Context = metricsService.keystoreResponseTimer.time()

  private def sessionID(implicit hc: HeaderCarrier): String = hc.sessionId.getOrElse(throw new RuntimeException("Active User had no Session ID")).value

  def cache[T](formId: String, regId: String, txId: String, body: T)
              (implicit hc: HeaderCarrier, format: Format[T], request: Request[_], tag: TypeTag[T]): Future[SessionMap] = {
    infoLog(s"[cache] attempting to cache ${tag.toString()}")
    metricsService.processDataResponseWithMetrics[SessionMap](successCounter, failedCounter, timer) {
      val updatedCacheMap = SessionMap(sessionID, regId, txId, Map(formId -> Json.toJson(body)))
      sessionRepository.upsertSessionMap(updatedCacheMap) map (_ => updatedCacheMap)
    }
  }

  def cacheSessionMap(map: SessionMap)(implicit request: Request[_]): Future[SessionMap] = {
    metricsService.processDataResponseWithMetrics[SessionMap](successCounter, failedCounter, timer) {
      sessionRepository.upsertSessionMap(map) map (_ => map)
    }
  }

  def fetch()(implicit hc: HeaderCarrier): Future[Option[SessionMap]] = {
    metricsService.processOptionalDataWithMetrics[SessionMap](successCounter, emptyResponseCounter, timer) {
      sessionRepository.getSessionMap(sessionID)
    }
  }

  def fetchAndGetFromKeystore(key: String)
                             (implicit hc: HeaderCarrier, format: Format[CurrentProfile], request: Request[_]): Future[Option[CurrentProfile]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, emptyResponseCounter, timer) {
      sessionCache.fetchAndGetEntry(key) flatMap { data =>
        data.fold(Future.successful(data)) { cp =>
          cache(key, cp.registrationID, cp.companyTaxRegistration.transactionId, cp).map(_ => data)
        }
      }
    }
  }


  def fetchAndGet[T](key: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    metricsService.processOptionalDataWithMetrics[T](successCounter, emptyResponseCounter, timer) {
      sessionRepository.getSessionMap(sessionID).map {
        _.flatMap(_.getEntry(key))
      }
    }
  }

  def fetchByTransactionId(txId: String): Future[Option[SessionMap]] = {
    metricsService.processOptionalDataWithMetrics(successCounter, emptyResponseCounter, timer) {
      sessionRepository.getLatestSessionMapByTransactionId(txId)
    }
  }

  def remove()(implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] = {
    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      sessionRepository.removeDocument(sessionID)
    }
  }
}
