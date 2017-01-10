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

package models.formModels

import models.payeRegistration.companyDetails.TradingName
import uk.gov.hmrc.play.test.UnitSpec

class TradingNameFormModelSpec extends UnitSpec {

  val tradingNameYesDataModel = TradingName(Some("tstName"))
  val tradingNameNoDataModel = TradingName(None)

  val tradingNameYesFormModel = TradingNameFormModel("yes", Some("tstName"))
  val tradingNameNoFormModel = TradingNameFormModel("no", None)
  val tradingNameNoWithNameFormModel = TradingNameFormModel("no", Some("tstName"))

  "Creating a Trading Name Form Model from a Data Model" should {
    "succeed for yes -> tradingName" in {
      new TradingNameFormModel(tradingNameYesDataModel) shouldBe tradingNameYesFormModel
    }
    "succeed for no -> no tradingName" in {
      new TradingNameFormModel(tradingNameNoDataModel) shouldBe tradingNameNoFormModel
    }
  }

  "Creating a Trading Name Data Model from a Form Model" should {
    "succeed for yes -> tradingName" in {
      tradingNameYesFormModel.toData shouldBe tradingNameYesDataModel
    }
    "succeed for no -> no tradingName" in {
      tradingNameNoFormModel.toData shouldBe tradingNameNoDataModel
    }
    "succeed for no -> tradingName" in {
      tradingNameNoWithNameFormModel.toData shouldBe tradingNameNoDataModel
    }
  }

}
