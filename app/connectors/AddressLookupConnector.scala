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
import config.WSHttp
import models.Address
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Call
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NoStackTrace

@Singleton
class AddressLookupConnector @Inject()(injMetricsService: MetricsService) extends AddressLookupConnect with ServicesConfig {
  val addressLookupFrontendUrl = baseUrl("address-lookup-frontend")
  lazy val payeRegistrationUrl = getConfString("paye-registration-frontend.www.url","")
  val http : WSHttp = WSHttp
  val metricsService = injMetricsService
  val successCounter = metricsService.addressLookupSuccessResponseCounter
  val failedCounter  = metricsService.addressLookupFailedResponseCounter
  def timer          = metricsService.addressLookupResponseTimer.time()
}

class ALFLocationHeaderNotSetException extends NoStackTrace

trait AddressLookupConnect {

  val addressLookupFrontendUrl: String
  val payeRegistrationUrl: String
  val http: WSHttp
  val metricsService: MetricsSrv

  val successCounter: Counter
  val failedCounter: Counter
  def timer: Timer.Context

  def getAddress(id: String)(implicit hc: HeaderCarrier) = {
    implicit val reads = Address.adressLookupReads
    metricsService.processDataResponseWithMetrics[Address](successCounter, failedCounter, timer) {
      http.GET[Address](s"$addressLookupFrontendUrl/api/confirmed?id=$id")
    }
  }

  def getOnRampUrl(query: String, call: Call)(implicit hc: HeaderCarrier): Future[String] = {
    val postUrl      = s"$addressLookupFrontendUrl/api/init/$query"
    val continue     = s"$payeRegistrationUrl${call.url}"
    val continueJson = Json.obj("continueUrl" -> s"$continue")

    metricsService.processHttpResponseWithMetrics(successCounter, failedCounter, timer) {
      http.POST[JsObject, HttpResponse](postUrl, continueJson)
    } map {
      _.header("Location").getOrElse {
        Logger.warn("[AddressLookupConnector] [getOnRampUrl] - ERROR: Location header not set in ALF response")
        throw new ALFLocationHeaderNotSetException
      }
    }
  }
}
