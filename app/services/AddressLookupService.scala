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
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{PAYEFeatureSwitch, PAYEFeatureSwitches}

@Singleton
class AddressLookupService @Inject()(
                                      injFeatureSwitch: PAYEFeatureSwitch,
                                      injAddressConnector: AddressLookupConnector)
  extends AddressLookupSrv with ServicesConfig {
  lazy val payeRegistrationUrl = getConfString("paye-registration-frontend.www.url","")
  lazy val payeRegistrationUri = getConfString("paye-registration-frontend.www.uri","")
  lazy val addressLookupFrontendUrl = getConfString("address-lookup-frontend.www.url","")
  lazy val addressLookupFrontendUri = getConfString("address-lookup-frontend.www.uri","")
  val addressLookupConnector = injAddressConnector
  val featureSwitch = injFeatureSwitch
}

trait AddressLookupSrv {

  val payeRegistrationUrl : String
  val payeRegistrationUri : String
  val addressLookupFrontendUrl: String
  val addressLookupFrontendUri: String
  val addressLookupConnector: AddressLookupConnect
  val featureSwitch: PAYEFeatureSwitches

  def buildAddressLookupUrl(query: String = "payereg1") = {
    useAddressLookupFrontend match {
      case true => addressLookupFrontendUrl + addressLookupFrontendUri + "/uk/addresses/" + query + s"?continue=" + payeRegistrationUrl + payeRegistrationUri + "/return-from-address"
      case false => payeRegistrationUrl + controllers.userJourney.routes.DirectorDetailsController.directorDetails().url
    }
  }

  def getAddress(id: String)(implicit hc: HeaderCarrier) = {
    addressLookupConnector.getAddress(id)
  }

  private[services] def useAddressLookupFrontend: Boolean = {
    featureSwitch.addressLookupFrontend.enabled
  }

}
