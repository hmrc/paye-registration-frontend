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

package helpers.mocks

import com.codahale.metrics.{Counter, Timer}
import com.codahale.metrics.MetricRegistry
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.MetricsService

trait MockMetrics extends MockitoSugar {

  lazy val mockContext = mock[Timer.Context]

  val mockTimer = new Timer()
  val mockCounter = mock[Counter]
  val mockMetricssss = mock[MetricRegistry]
  when(mockMetricssss.timer(any())).thenReturn(mockTimer)
  when(mockMetricssss.counter(any())).thenReturn(mockCounter)

  val metricsServiceTestttt = new MetricsService(
    mockMetricssss
  ) {
    override val payeRegistrationResponseTimer = mockTimer
    override val addressLookupResponseTimer = mockTimer
    override val businessRegistrationResponseTimer = mockTimer
    override val incorpInfoResponseTimer = mockTimer
    override val companyRegistrationResponseTimer = mockTimer
    override val keystoreResponseTimer = mockTimer
    override val s4lResponseTimer = mockTimer
    override val deskproResponseTimer = mockTimer
    override val keystoreSuccessResponseCounter = mockCounter
    override val keystoreEmptyResponseCounter = mockCounter
    override val keystoreFailedResponseCounter = mockCounter
    override val s4lSuccessResponseCounter = mockCounter
    override val s4lEmptyResponseCounter = mockCounter
    override val s4lFailedResponseCounter = mockCounter
    override val companyDetailsSuccessResponseCounter = mockCounter
    override val companyDetailsFailedResponseCounter = mockCounter
    override val addressLookupSuccessResponseCounter = mockCounter
    override val addressLookupFailedResponseCounter = mockCounter
  }
}
