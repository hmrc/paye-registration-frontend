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
import connectors.CoHoAPIConnector._
import models.external.CoHoCompanyDetailsModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.{HttpPost, HttpGet, HttpResponse, HeaderCarrier}

import scala.concurrent.Future

object TestCoHoAPIConnector extends TestCoHoAPIConnector {
  //$COVERAGE-OFF$
  val coHoAPIUrl = baseUrl("coho-api")
  val http = WSHttp
  //$COVERAGE-ON$
}

trait TestCoHoAPIConnector {

  val coHoAPIUrl: String
  val http: HttpGet with HttpPost

  def addCoHoCompanyDetails(coHoCompanyDetailsModel: CoHoCompanyDetailsModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val json = Json.toJson[CoHoCompanyDetailsModel](coHoCompanyDetailsModel)
    http.POST[JsValue, HttpResponse](s"$coHoAPIUrl/incorporation-frontend-stubs/test-only/insert-company-details", json)
  }

  def tearDownCoHoCompanyDetails()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$coHoAPIUrl/incorporation-frontend-stubs/test-only/wipe-company-details")
  }

}
