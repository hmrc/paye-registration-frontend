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

package services

import javax.inject.{Inject, Singleton}

import com.codahale.metrics.Timer
import com.kenshoo.play.metrics.Metrics

@Singleton
class MetricsService @Inject()(injMetrics: Metrics) extends MetricsSrv {
  override val payeRegistrationResponseTimer = injMetrics.defaultRegistry.timer("paye-registration-call-timer")
  override val addressLookupResponseTimer = injMetrics.defaultRegistry.timer("address-lookup-call-timer")
  override val businessRegistrationResponseTimer = injMetrics.defaultRegistry.timer("business-registration-call-timer")
  override val cohoAPIResponseTimer = injMetrics.defaultRegistry.timer("coho-api-call-timer")
  override val companyRegistrationResponseTimer = injMetrics.defaultRegistry.timer("company-registration-call-timer")
  override val keystoreResponseTimer = injMetrics.defaultRegistry.timer("keystore-call-timer")
  override val s4lResponseTimer = injMetrics.defaultRegistry.timer("s4l-call-timer")
  override val deskproResponseTimer = injMetrics.defaultRegistry.timer("deskpro-call-timer")
}

trait MetricsSrv {
  val payeRegistrationResponseTimer: Timer
  val addressLookupResponseTimer: Timer
  val businessRegistrationResponseTimer: Timer
  val cohoAPIResponseTimer: Timer
  val companyRegistrationResponseTimer: Timer
  val keystoreResponseTimer: Timer
  val s4lResponseTimer: Timer
  val deskproResponseTimer: Timer
}
