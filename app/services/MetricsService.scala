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

package services

import javax.inject.Inject

import com.codahale.metrics.{Counter, Timer}
import com.kenshoo.play.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}

class MetricsServiceImpl @Inject()(metrics: Metrics) extends MetricsService {
  override val payeRegistrationResponseTimer          = metrics.defaultRegistry.timer("paye-registration-call-timer")
  override val addressLookupResponseTimer             = metrics.defaultRegistry.timer("address-lookup-call-timer")
  override val businessRegistrationResponseTimer      = metrics.defaultRegistry.timer("business-registration-call-timer")
  override val incorpInfoResponseTimer                = metrics.defaultRegistry.timer("incorporation-information-call-timer")
  override val companyRegistrationResponseTimer       = metrics.defaultRegistry.timer("company-registration-call-timer")
  override val keystoreResponseTimer                  = metrics.defaultRegistry.timer("keystore-call-timer")
  override val s4lResponseTimer                       = metrics.defaultRegistry.timer("s4l-call-timer")
  override val deskproResponseTimer                   = metrics.defaultRegistry.timer("deskpro-call-timer")

  override val keystoreSuccessResponseCounter         = metrics.defaultRegistry.counter("keystore-success-response-counter")
  override val keystoreEmptyResponseCounter           = metrics.defaultRegistry.counter("keystore-empty-response-counter")
  override val keystoreFailedResponseCounter          = metrics.defaultRegistry.counter("keystore-failed-response-counter")

  override val s4lSuccessResponseCounter              = metrics.defaultRegistry.counter("s4l-success-response-counter")
  override val s4lEmptyResponseCounter                = metrics.defaultRegistry.counter("s4l-empty-response-counter")
  override val s4lFailedResponseCounter               = metrics.defaultRegistry.counter("s4l-failed-response-counter")

  override val companyDetailsSuccessResponseCounter   = metrics.defaultRegistry.counter("coho-ro-address-success-response-counter")
  override val companyDetailsFailedResponseCounter    = metrics.defaultRegistry.counter("coho-ro-address-failed-response-counter")

  override val addressLookupSuccessResponseCounter    = metrics.defaultRegistry.counter("address-lookup-success-response-counter")
  override val addressLookupFailedResponseCounter     = metrics.defaultRegistry.counter("address-lookup-failed-response-counter")
}

trait MetricsService {
  val payeRegistrationResponseTimer: Timer
  val addressLookupResponseTimer: Timer
  val businessRegistrationResponseTimer: Timer
  val incorpInfoResponseTimer: Timer
  val companyRegistrationResponseTimer: Timer
  val keystoreResponseTimer: Timer
  val s4lResponseTimer: Timer
  val deskproResponseTimer: Timer

  val keystoreSuccessResponseCounter: Counter
  val keystoreEmptyResponseCounter: Counter
  val keystoreFailedResponseCounter: Counter

  val s4lSuccessResponseCounter: Counter
  val s4lEmptyResponseCounter: Counter
  val s4lFailedResponseCounter: Counter

  val companyDetailsSuccessResponseCounter: Counter
  val companyDetailsFailedResponseCounter: Counter

  val addressLookupSuccessResponseCounter: Counter
  val addressLookupFailedResponseCounter: Counter


  def processDataResponseWithMetrics[T](success: Counter, failed: Counter, timer: Timer.Context)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    f map { data =>
      timer.stop()
      success.inc(1)
      data
    } recover {
      case e =>
        timer.stop()
        failed.inc(1)
        throw e
    }
  }

  def processOptionalDataWithMetrics[T](success: Counter, failed: Counter, timer: Timer.Context)(f: => Future[Option[T]])(implicit ec: ExecutionContext): Future[Option[T]] = {
    f map { data =>
      timer.stop()
      processOptionalDataResponse[T](data)(success, failed)
    }recover{
      case e =>
        timer.stop()
        failed.inc(1)
        throw e
    }
  }

  private def processOptionalDataResponse[T](data: Option[T])(successCount: Counter, failedCount: Counter): Option[T] = {
    data match {
      case Some(x) => successCount.inc(1); Some(x)
      case _       => failedCount.inc(1); None
    }
  }
}
