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

package models.payeRegistration

import models.payeRegistration.companyDetails.{TradingName, CompanyDetails}
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class CompanyDetailsSpec extends UnitSpec {

  val tstCompletedTradingName = TradingName(Some("tst trading name"))

  val tstModel = CompanyDetails(
    crn = Some("tstCRN"),
    companyName = "Test Company",
    tradingName = Some(tstCompletedTradingName)
  )

  val tstJson = Json.parse(
    """{
      | "crn":"tstCRN",
      | "companyName":"Test Company",
      | "tradingName":"tst trading name"
    }""".stripMargin
  )

  val tstSparseJson = Json.parse(
    """{
      | "companyName":"Test Company"
    }""".stripMargin
  )
  val tstSparseModel = CompanyDetails(
    crn = None,
    companyName = "Test Company",
    tradingName = None
  )


  val tstTradingNameNoModel = CompanyDetails(
    crn = None,
    companyName = "Test Company",
    tradingName = Some(TradingName(None))
  )

  "Company Details" should {
    "read a full model from Json" in {
      Json.fromJson[CompanyDetails](tstJson).asOpt shouldBe Some(tstModel)
    }
    "write a full model to Json" in {
      Json.toJson[CompanyDetails](tstModel) shouldBe tstJson
    }

    "read a model with no crn or trading name from Json" in {
      Json.fromJson[CompanyDetails](tstSparseJson).asOpt shouldBe Some(tstSparseModel)
    }
    "write a model with no trading name to Json" in {
      Json.toJson[CompanyDetails](tstSparseModel) shouldBe tstSparseJson
    }
    "write a model with no trading name recorded as No to Json" in {
      Json.toJson[CompanyDetails](tstTradingNameNoModel) shouldBe tstSparseJson
    }
  }

}
