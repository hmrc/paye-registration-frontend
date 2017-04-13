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

import javax.inject.{Inject, Singleton}

import config.WSHttp
import models.external.CoHoCompanyDetailsModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class TestCoHoAPIConnector @Inject()() extends TestCoHoAPIConnect with ServicesConfig {
  val coHoAPIUrl = baseUrl("coho-api")
  val http : WSHttp = WSHttp
}

trait TestCoHoAPIConnect {

  val coHoAPIUrl: String
  val http: WSHttp

  def addCoHoCompanyDetails(coHoCompanyDetailsModel: CoHoCompanyDetailsModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val json = Json.toJson[CoHoCompanyDetailsModel](coHoCompanyDetailsModel)
    http.POST[JsValue, HttpResponse](s"$coHoAPIUrl/incorporation-frontend-stubs/test-only/insert-company-details", json)
  }

  def tearDownCoHoCompanyDetails(regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$coHoAPIUrl/incorporation-frontend-stubs/test-only/wipe-individual-company-details/$regId")
  }
}
