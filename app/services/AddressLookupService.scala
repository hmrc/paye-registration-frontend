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

import connectors.{AddressLookupConnect, AddressLookupConnector}
import models.Address
import play.api.Logger
import play.api.libs.json.{JsArray, JsObject}
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{PAYEFeatureSwitch, PAYEFeatureSwitches}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AddressLookupService @Inject()(injFeatureSwitch: PAYEFeatureSwitch,
                                     injAddressConnector: AddressLookupConnector,
                                     injMetrics: MetricsService)
  extends AddressLookupSrv with ServicesConfig {
  lazy val payeRegistrationUrl = getConfString("paye-registration-frontend.www.url","")
  val addressLookupConnector = injAddressConnector
  val featureSwitch = injFeatureSwitch
  val metricsService: MetricsSrv = injMetrics
}

trait AddressLookupSrv {

  val payeRegistrationUrl : String
  val addressLookupConnector: AddressLookupConnect
  val featureSwitch: PAYEFeatureSwitches
  val metricsService: MetricsSrv

  def buildAddressLookupUrl(query: String, call: Call)(implicit hc: HeaderCarrier): Future[String] = {
    useAddressLookupFrontend match {
      case true => addressLookupConnector.getOnRampUrl(query, call)
      case false =>
        call.url.split('/').last match {
          case "return-from-address-for-ppob" => Future.successful(controllers.test.routes.TestAddressLookupController.noLookupPPOBAddress().url)
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
