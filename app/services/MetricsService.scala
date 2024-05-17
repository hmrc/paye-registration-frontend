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

package services


import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import play.api.mvc.Request
import utils.Logging

import scala.reflect.runtime.universe._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MetricsService @Inject()(metrics: MetricRegistry) extends Logging {

  val payeRegistrationResponseTimer: Timer = metrics.timer("paye-registration-call-timer")
  val addressLookupResponseTimer: Timer = metrics.timer("address-lookup-call-timer")
  val businessRegistrationResponseTimer: Timer = metrics.timer("business-registration-call-timer")
  val incorpInfoResponseTimer: Timer = metrics.timer("incorporation-information-call-timer")
  val companyRegistrationResponseTimer: Timer = metrics.timer("company-registration-call-timer")
  val keystoreResponseTimer: Timer = metrics.timer("keystore-call-timer")
  val s4lResponseTimer: Timer = metrics.timer("s4l-call-timer")
  val deskproResponseTimer: Timer = metrics.timer("deskpro-call-timer")

  val keystoreSuccessResponseCounter: Counter = metrics.counter("keystore-success-response-counter")
  val keystoreEmptyResponseCounter: Counter = metrics.counter("keystore-empty-response-counter")
  val keystoreFailedResponseCounter: Counter = metrics.counter("keystore-failed-response-counter")

  val s4lSuccessResponseCounter: Counter = metrics.counter("s4l-success-response-counter")
  val s4lEmptyResponseCounter: Counter = metrics.counter("s4l-empty-response-counter")
  val s4lFailedResponseCounter: Counter = metrics.counter("s4l-failed-response-counter")

  val companyDetailsSuccessResponseCounter: Counter = metrics.counter("coho-ro-address-success-response-counter")
 val companyDetailsFailedResponseCounter: Counter = metrics.counter("coho-ro-address-failed-response-counter")

  val addressLookupSuccessResponseCounter: Counter = metrics.counter("address-lookup-success-response-counter")
  val addressLookupFailedResponseCounter: Counter = metrics.counter("address-lookup-failed-response-counter")

  def processDataResponseWithMetrics1[T](timer: Timer.Context)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    f map { data =>
      timer.stop()
      data
    } recover {
      case e =>
        timer.stop()
        throw e
    }
  }

  def processDataResponseWithMetrics[T](success: Counter, failed: Counter, timer: Timer.Context)(f: => Future[T])
                                       (implicit ec: ExecutionContext, request: Request[_], tag: TypeTag[T]): Future[T] = {
    f map { data =>
      timer.stop()
      success.inc(1)
      data
    } recover {
      case e =>
        errorLog(s"[processDataResponseWithMetrics] failed to process data response with metrics for ${tag.toString()}")
        timer.stop()
        failed.inc(1)
        throw e
    }
  }

  def processOptionalDataWithMetrics[T](success: Counter, failed: Counter, timer: Timer.Context)(f: => Future[Option[T]])(implicit ec: ExecutionContext): Future[Option[T]] = {
    f map { data =>
      timer.stop()
      processOptionalDataResponse[T](data)(success, failed)
    } recover {
      case e =>
        timer.stop()
        failed.inc(1)
        throw e
    }
  }

  private def processOptionalDataResponse[T](data: Option[T])(successCount: Counter, failedCount: Counter): Option[T] = {
    data match {
      case Some(x) => successCount.inc(1); Some(x)
      case _ => failedCount.inc(1); None
    }
  }
}
