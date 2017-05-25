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

package mocks

import com.codahale.metrics.{Counter, Timer}
import org.scalatest.mockito.MockitoSugar
import services.MetricsSrv

class MockMetrics extends MetricsSrv with MockitoSugar {
  lazy val mockContext = mock[Timer.Context]
  val mockTimer = new Timer()
  val mockCounter = mock[Counter]


  override val payeRegistrationResponseTimer = mockTimer
  override val addressLookupResponseTimer = mockTimer
  override val businessRegistrationResponseTimer = mockTimer
  override val incorpInfoResponseTimer = mockTimer
  override val companyRegistrationResponseTimer = mockTimer
  override val keystoreResponseTimer = mockTimer
  override val s4lResponseTimer = mockTimer
  override val deskproResponseTimer = mockTimer
}
