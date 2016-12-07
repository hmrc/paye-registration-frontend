/*
 * Copyright 2016 HM Revenue & Customs
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

package forms.companyDetails

import uk.gov.hmrc.play.test.UnitSpec
import common.exceptions.InternalExceptions._

class TradingNameFormSpec extends UnitSpec {

  val testForm = TradingNameForm.form
  val validData = Map(
    "tradeUnderDifferentName" -> "yes",
    "tradingName" -> "Tradez R Us"
  )
  val invalidData = Map(
    "tradeUnderDifferentName" -> "yes"
  )
  val noValidationNeedData = Map(
    "tradeUnderDifferentName" -> "no"
  )

  "Validating the trading name form" should {
    "throw the correct exception for an incorrect form" in {
      a [ExpectedFormFieldNotPopulatedException] should be thrownBy  TradingNameForm.validateForm(testForm)
    }

    "return the original form if no validation is needed" in {
      TradingNameForm.validateForm(testForm.bind(noValidationNeedData)) shouldBe testForm.bind(noValidationNeedData)
    }

    "return the original form if data is correct" in {
      TradingNameForm.validateForm(testForm.bind(validData)) shouldBe testForm.bind(validData)
    }

    "return the form with error for incomplete data" in {
      TradingNameForm.validateForm(testForm.bind(invalidData)) shouldBe testForm.bind(invalidData).withError("tradingName", "pages.tradingName.errorQuestion")
    }
  }

}
