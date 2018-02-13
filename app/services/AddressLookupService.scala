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

package services

import javax.inject.Inject

import connectors.AddressLookupConnector
import models.Address
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.{PAYEFeatureSwitch, PAYEFeatureSwitches}

import scala.concurrent.Future

class AddressLookupServiceImpl @Inject()(val featureSwitch: PAYEFeatureSwitch,
                                         val addressLookupConnector: AddressLookupConnector,
                                         servicesConfig: ServicesConfig,
                                         val metricsService: MetricsService) extends AddressLookupService {
  lazy val payeRegistrationUrl = servicesConfig.getConfString("paye-registration-frontend.www.url","")
}

trait AddressLookupService {
  val payeRegistrationUrl : String
  val addressLookupConnector: AddressLookupConnector
  val featureSwitch: PAYEFeatureSwitches
  val metricsService: MetricsService

  def buildAddressLookupUrl(key: String, call: Call)(implicit hc: HeaderCarrier): Future[String] = {
    if(useAddressLookupFrontend) {
      addressLookupConnector.getOnRampUrl(key, call)
    } else {
      call.url.split('/').last match {
        case "return-from-address-for-ppob"         => Future.successful(controllers.test.routes.TestAddressLookupController.noLookupPPOBAddress().url)
        case "return-from-address-for-corresp-addr" => Future.successful(controllers.test.routes.TestAddressLookupController.noLookupCorrespondenceAddress().url)
      }
    }
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

  private[services] def useAddressLookupFrontend: Boolean = {
    featureSwitch.addressLookupFrontend.enabled
  }
}
