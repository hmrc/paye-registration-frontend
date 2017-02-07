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
  val payeRegistrationUrl = baseUrl("paye-registration-frontend")
  val addressLookupFrontendUrl = baseUrl("address-lookup-frontend")
  val addressLookupConnector = injAddressConnector
  val featureSwitch = injFeatureSwitch
}

trait AddressLookupSrv {

  val payeRegistrationUrl : String
  val addressLookupFrontendUrl: String
  val addressLookupConnector: AddressLookupConnect
  val featureSwitch: PAYEFeatureSwitches

  def buildAddressLookupUrl(query: String = "payereg1") = {
    val url = useAddressLookupFrontend match {
      case true => addressLookupFrontendUrl + "/lookup-address/uk/addresses/"
      case false => controllers.userJourney.routes.EmploymentController.employingStaff().url
    }

    url + query + s"?continue=" + payeRegistrationUrl + "/register-for-paye/return-from-address"
  }

  def getAddress(id: String)(implicit hc: HeaderCarrier) = {
    addressLookupConnector.getAddress(id)
  }

  private[services] def useAddressLookupFrontend: Boolean = {
    featureSwitch.addressLookupFrontend.enabled
  }

}
