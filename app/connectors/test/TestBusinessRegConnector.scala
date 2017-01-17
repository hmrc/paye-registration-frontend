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

package connectors.test

import config.WSHttp
import connectors.BusinessRegistrationConnector._
import models.external.{BusinessRegistrationRequest, CurrentProfile}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.{HttpPost, HttpGet, HeaderCarrier}

import scala.concurrent.Future

object TestBusinessRegConnector extends TestBusinessRegConnector {
  //$COVERAGE-OFF$
  val businessRegUrl = baseUrl("business-registration")
  val http = WSHttp
  //$COVERAGE-ON$
}

trait TestBusinessRegConnector {

  val businessRegUrl: String
  val http: HttpGet with HttpPost

  def createCurrentProfileEntry(implicit hc: HeaderCarrier): Future[CurrentProfile] = {
    val json = Json.toJson[BusinessRegistrationRequest](BusinessRegistrationRequest("ENG"))
    http.POST[JsValue, CurrentProfile](s"$businessRegUrl/business-registration/business-tax-registration", json)
  }

}
