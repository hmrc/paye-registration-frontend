/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.AddressLookupConnector
import javax.inject.Inject
import models.Address
import play.api.mvc.{Call, Request}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.{PAYEFeatureSwitch, PAYEFeatureSwitches}

import scala.concurrent.Future

class AddressLookupServiceImpl @Inject()(val featureSwitch: PAYEFeatureSwitch,
                                         val addressLookupConnector: AddressLookupConnector,
                                         val metricsService: MetricsService,
                                         override val runModeConfiguration: Configuration, environment: Environment) extends AddressLookupService with ServicesConfig {
  lazy val payeRegistrationUrl = getConfString("paye-registration-frontend.www.url", "")

  override protected def mode = environment.mode
}

trait AddressLookupService {
  val payeRegistrationUrl: String
  val addressLookupConnector: AddressLookupConnector
  val featureSwitch: PAYEFeatureSwitches
  val metricsService: MetricsService

  def buildAddressLookupUrl(key: String, call: Call)(implicit hc: HeaderCarrier): Future[String] = {
    addressLookupConnector.getOnRampUrl(key, call)
  }

  def getAddress(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[Address]] = {
    request.getQueryString("id") match {
      case Some(id) =>
        val addressLookUpTimer = metricsService.addressLookupResponseTimer.time()
        addressLookupConnector.getAddress(id) map { addr =>
          addressLookUpTimer.stop()
          Some(addr)
        }
      case None => Future.successful(None)
    }
  }
}
