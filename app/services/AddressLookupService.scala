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

import connectors.AddressLookupConnector
import models.Address
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AddressLookupService @Inject()(addressLookupConnector: AddressLookupConnector,
                                     addressLookupConfigBuilderService: AddressLookupConfigBuilderService) {

  def buildAddressLookupUrl(key: String, call: Call)(implicit hc: HeaderCarrier, messages: Messages, request: Request[_]): Future[String] = {
    val alfJourneyConfig = addressLookupConfigBuilderService.buildConfig(call, key)

    addressLookupConnector.getOnRampUrl(alfJourneyConfig)
  }

  def getAddress(id: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Address] = addressLookupConnector.getAddress(id)
}
