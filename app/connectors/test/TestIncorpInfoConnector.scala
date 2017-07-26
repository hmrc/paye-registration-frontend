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
class TestIncorpInfoConnector @Inject()() extends TestIncorpInfoConnect with ServicesConfig {
  val incorpFEStubsUrl = baseUrl("incorporation-frontend-stubs")
  val http : WSHttp = WSHttp
}

trait TestIncorpInfoConnect {

  val incorpFEStubsUrl: String
  val http: WSHttp

  private def txId(regId: String):String = s"000-434-$regId"

  def setupCoHoCompanyDetails(regId: String, companyName: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val officers = Json.parse(
      s"""
         |{
         |    "transaction_id" : "${txId(regId)}",
         |    "company_name" : "$companyName",
         |    "company_type" : "ltd",
         |    "registered_office_address" : {
         |        "premises" : "12",
         |        "address_line_1" : "Test Road",
         |        "address_line_2" : "Testshire",
         |        "locality" : "Greater Testford",
         |        "country" : "United Kingdom",
         |        "postal_code" : "TE1 1ST"
         |    },
         |    "officers" : [
         |        {
         |            "name_elements" : {
         |                "forename" : "Bob",
         |                "other_forenames" : "Bimbly Bobblous",
         |                "surname" : "Bobbings"
         |            },
         |            "date_of_birth" : {
         |                "day" : "12",
         |                "month" : "11",
         |                "year" : "1973"
         |            },
         |            "address" : {
         |                "premises" : "13",
         |                "address_line_1" : "Test Road",
         |                "address_line_2" : "Testshire",
         |                "locality" : "Greater Testford",
         |                "country" : "United Kingdom",
         |                "postal_code" : "TE1 1ST"
         |            },
         |            "officer_role" : "director"
         |        },
         |        {
         |            "name_elements" : {
         |                "title" : "Mx",
         |                "forename" : "Jingly",
         |                "surname" : "Jingles"
         |            },
         |            "date_of_birth" : {
         |                "day" : "12",
         |                "month" : "07",
         |                "year" : "1988"
         |            },
         |            "address" : {
         |                "premises" : "14",
         |                "address_line_1" : "Test Road",
         |                "address_line_2" : "Testshire",
         |                "locality" : "Greater Testford",
         |                "country" : "United Kingdom",
         |                "postal_code" : "TE1 1ST"
         |            },
         |            "officer_role" : "director"
         |        },
         |        {
         |            "name_elements" : {
         |                "forename" : "Jorge",
         |                "surname" : "Freshwater"
         |            },
         |            "date_of_birth" : {
         |                "day" : "10",
         |                "month" : "06",
         |                "year" : "1994"
         |            },
         |            "address" : {
         |                "premises" : "15",
         |                "address_line_1" : "Test Road",
         |                "address_line_2" : "Testshire",
         |                "locality" : "Greater Testford",
         |                "country" : "United Kingdom",
         |                "postal_code" : "TE1 1ST"
         |            },
         |            "officer_role" : "director"
         |        },
         |        {
         |            "name_elements" : {
         |                "forename" : "Jesus",
         |                "surname" : "Splitwater"
         |            },
         |            "date_of_birth" : {
         |                "day" : "25",
         |                "month" : "12",
         |                "year" : "1990"
         |            },
         |            "address" : {
         |                "premises" : "16",
         |                "address_line_1" : "Test Road",
         |                "address_line_2" : "Testshire",
         |                "locality" : "Greater Testford",
         |                "country" : "United Kingdom",
         |                "postal_code" : "TE1 1ST"
         |            },
         |            "officer_role" : "director",
         |            "resigned_on" : "2017-01-01"
         |        }
         |    ],
         |    "sic_codes" : [
         |        {
         |            "sic_description" : "Public order and safety activities",
         |            "sic_code" : "84240"
         |        },
         |        {
         |            "sic_description" : "Raising of dairy cattle",
         |            "sic_code" : "01410"
         |        }
         |    ]
         |}
    """.stripMargin)

    http.POST[JsValue, HttpResponse](s"$incorpFEStubsUrl/incorporation-frontend-stubs/insert-data", officers)
  }

  def teardownCoHoCompanyDetails()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.PUT[String, HttpResponse](s"$incorpFEStubsUrl/incorporation-frontend-stubs/wipe-data", "")
  }

  def teardownIndividualCoHoCompanyDetails(regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.PUT[String, HttpResponse](s"$incorpFEStubsUrl/incorporation-frontend-stubs/wipe-individual-data", txId(regId))
  }
}
