/*
 * Copyright 2021 HM Revenue & Customs
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

import config.{AppConfig, WSHttp}
import javax.inject.Inject
import models.external.{BusinessProfile, BusinessRegistrationRequest}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{CorePost, HeaderCarrier}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class TestBusinessRegConnectorImpl @Inject()(val http: WSHttp,
                                             appConfig: AppConfig) extends TestBusinessRegConnector {
  val businessRegUrl = appConfig.servicesConfig.baseUrl("business-registration")
}

trait TestBusinessRegConnector {
  val businessRegUrl: String
  val http: CorePost

  def createBusinessProfileEntry(implicit hc: HeaderCarrier): Future[BusinessProfile] = {
    val json = Json.toJson[BusinessRegistrationRequest](BusinessRegistrationRequest("ENG"))
    http.POST[JsValue, BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration", json)
  }

  def updateCompletionCapacity(regId: String, completionCapacity: String)(implicit hc: HeaderCarrier): Future[String] = {
    val json = Json.parse(s"""{"completionCapacity" : "$completionCapacity"}""".stripMargin)
    http.POST[JsValue, JsValue](s"$businessRegUrl/business-registration/test-only/update-cc/$regId", json) map (_.toString)
  }
}
