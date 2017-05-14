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
  val incorpInfoUrl = baseUrl("incorporation-frontend-stubs")
  val http : WSHttp = WSHttp
}

trait TestIncorpInfoConnect {

  val incorpInfoUrl: String
  val http: WSHttp

  def addCoHoCompanyDetails(coHoCompanyDetailsModel: CoHoCompanyDetailsModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val json = Json.toJson[CoHoCompanyDetailsModel](coHoCompanyDetailsModel)
    http.POST[JsValue, HttpResponse](s"$incorpInfoUrl/incorporation-frontend-stubs/test-only/insert-company-details", json)
  }

  def tearDownCoHoCompanyDetails(regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$incorpInfoUrl/incorporation-frontend-stubs/test-only/wipe-individual-company-details/$regId")
  }

  def setupOfficers(regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val officers = Json.parse(
      s"""
         |{
         |    "transaction_id" : "000-434-$regId",
         |    "company_name" : "MOOO LIMITED",
         |    "company_type" : "ltd",
         |    "registered_office_address" : {
         |        "premises" : "98",
         |        "address_line_1" : "LIMBRICK LANE",
         |        "address_line_2" : "GORING-BY-SEA",
         |        "locality" : "WORTHING",
         |        "country" : "United Kingdom",
         |        "postal_code" : "BN12 6AG"
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
         |                "premises" : "98",
         |                "address_line_1" : "LIMBRICK LANE",
         |                "address_line_2" : "GORING-BY-SEA",
         |                "locality" : "WORTHING",
         |                "country" : "United Kingdom",
         |                "postal_code" : "BN12 6AG"
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
         |                "premises" : "713",
         |                "address_line_1" : "ST. JAMES GATE",
         |                "locality" : "NEWCASTLE UPON TYNE",
         |                "country" : "England",
         |                "postal_code" : "NE1 4BB"
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
         |                "premises" : "1",
         |                "address_line_1" : "L ST",
         |                "locality" : "TYNE",
         |                "country" : "England",
         |                "postal_code" : "AA1 4AA"
         |            },
         |            "officer_role" : "director"
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

    http.POST[JsValue, HttpResponse](s"$incorpInfoUrl/incorporation-frontend-stubs/insert-data", officers)
  }

  def teardownOfficers()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.PUT[String, HttpResponse](s"$incorpInfoUrl/incorporation-frontend-stubs/wipe-data", "")
  }
}
