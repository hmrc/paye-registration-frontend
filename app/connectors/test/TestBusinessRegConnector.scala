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

package connectors.test

import config.AppConfig
import connectors.BaseConnector
import connectors.httpParsers.BusinessRegistrationHttpParsers
import models.external.{BusinessProfile, BusinessRegistrationRequest}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestBusinessRegConnectorImpl @Inject()(val http: HttpClient,
                                             appConfig: AppConfig)(implicit val ec: ExecutionContext) extends TestBusinessRegConnector {
  val businessRegUrl = appConfig.servicesConfig.baseUrl("business-registration")
}

trait TestBusinessRegConnector extends BaseConnector with BusinessRegistrationHttpParsers {
  implicit val ec: ExecutionContext
  val businessRegUrl: String
  val http: CorePost

  def createBusinessProfileEntry(implicit hc: HeaderCarrier, request: Request[_]): Future[BusinessProfile] =
    http.POST[BusinessRegistrationRequest, BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration", BusinessRegistrationRequest("ENG"))(
      BusinessRegistrationRequest.formats, businessProfileHttpReads.map(_.get), hc, ec
    )

  def updateCompletionCapacity(regId: String, completionCapacity: String)(implicit hc: HeaderCarrier): Future[String] = {
    val json = Json.parse(s"""{"completionCapacity" : "$completionCapacity"}""".stripMargin)
    http.POST[JsValue, HttpResponse](s"$businessRegUrl/business-registration/test-only/update-cc/$regId", json)(implicitly, rawReads, hc, ec) map (_.body)
  }
}
