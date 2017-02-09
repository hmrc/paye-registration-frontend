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

import com.google.inject.{Inject, Singleton}
import config.WSHttp
import play.api.Logger
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AddressLookupConnector @Inject()() extends AddressLookupConnect with ServicesConfig {
  val addressLookupFrontendUrl = baseUrl("address-lookup-frontend.api")
  val http : WSHttp = WSHttp
}

trait AddressLookupConnect {

  val addressLookupFrontendUrl: String
  val http: WSHttp

  def getAddress(id: String)(implicit hc: HeaderCarrier) = {
    http.GET[JsObject](s"$addressLookupFrontendUrl/lookup-address/outcome/payereg1/$id") map {
      res =>
        Logger.info(res.toString)
        res
    }
  }
}
