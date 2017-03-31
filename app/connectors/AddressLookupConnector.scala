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

import config.WSHttp
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Call
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AddressLookupConnector @Inject()() extends AddressLookupConnect with ServicesConfig {
  val addressLookupFrontendUrl = baseUrl("address-lookup-frontend.api")
  lazy val addressLookupFrontendUri = getConfString("address-lookup-frontend.www.uri","")
  lazy val payeRegistrationUrl = getConfString("paye-registration-frontend.www.url","")
  val http : WSHttp = WSHttp
}

trait AddressLookupConnect {

  val addressLookupFrontendUrl: String
  val addressLookupFrontendUri: String
  val payeRegistrationUrl: String
  val http: WSHttp

  def getAddress(id: String)(implicit hc: HeaderCarrier) = {
    http.GET[JsObject](s"$addressLookupFrontendUrl/lookup-address/outcome/payereg1/$id")
  }

  def getOnRampUrl(query: String, call: Call)(implicit hc: HeaderCarrier): Future[String] = {
    val postUrl = s"$addressLookupFrontendUrl$addressLookupFrontendUri/init/$query"
    val continue = s"$payeRegistrationUrl${call.url}"
    val continueJson =
      Json.parse(s"""
       |{
       |   "continueUrl" : "$continue"
       |}
      """.stripMargin)

    http.POST[JsValue, String](postUrl, continueJson, Seq("CsrfToken" -> "noCheck")) map { resp =>
      Logger.debug(s"URL FROM ALF $resp")
      resp
    }
  }
}
