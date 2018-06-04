/*
 * Copyright 2018 HM Revenue & Customs
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

import common.exceptions.InternalExceptions._
import helpers.PayeComponentSpec
import models.view.TradingName

class TradingNameFormSpec extends PayeComponentSpec {

  val testForm = TradingNameForm.form
  val validData = Map(
    "differentName" -> "true",
    "tradingName" -> "Tradez R Us"
  )

  val validDataWithPunct = Map(
    "differentName" -> "true",
    "tradingName" -> "TestTrading&()-CJ.!"
  )

  val invalidData = Map(
    "differentName" -> "true"
  )

  val invalidDataTooLong = Map(
    "differentName" -> "true",
    "tradingName"   -> "A trading name that appears to be far too long for the box that it will go into"
  )

  val noValidationNeedData = Map(
    "differentName" -> "false"
  )

  "Validating the trading name form" should {
    "throw the correct exception for an incorrect form" in {
      a[ExpectedFormFieldNotPopulatedException] should be thrownBy  TradingNameForm.validateForm(testForm)
    }

    "return the original form if no validation is needed" in {
      TradingNameForm.validateForm(testForm.bind(noValidationNeedData)) mustBe testForm.bind(noValidationNeedData)
    }

    "return the original form if data is correct" in {
      TradingNameForm.validateForm(testForm.bind(validData)) mustBe testForm.bind(validData)
    }

    "return the original form if the data is correct even if it contains punctuation" in {
      TradingNameForm.validateForm(testForm.bind(validDataWithPunct)) mustBe testForm.bind(validDataWithPunct)
    }

    "return the form with error for incomplete data" in {
      TradingNameForm.validateForm(testForm.bind(invalidData)) mustBe testForm.bind(invalidData).withError("tradingName", "pages.tradingName.errorQuestion")
    }

    "return the form with errors as the trading name is too long" in {
      TradingNameForm.validateForm(testForm.bind(invalidDataTooLong)) mustBe testForm.bind(invalidDataTooLong).withError("tradingName", "pages.tradingName.error.length")
    }
  }
  "fillWithPrePop" should {
    "return a form with just trading Name populated and differentName set to blank string if TradingName model = None" in {
      val mappingOfForm = Map(
        "differentName" -> "",
        "tradingName" -> "foo bar wizz pre pop"
      )
      val prePopName = Some("foo bar wizz pre pop")
      val tradingNameModel = None

      val res = TradingNameForm.fillWithPrePop(prePopName,tradingNameModel)
      res.errors mustBe Seq.empty
      res mustBe testForm.bind(mappingOfForm).discardingErrors
    }
    "return a form populated with the model not the pre pop trading name as the model = Some and answer is true" in {
      val mappingOfForm = Map(
        "differentName" -> "true",
        "tradingName" -> "foo"
      )
      val prePopName = Some("foo bar wizz pre pop")
      val tradingNameModel = Some(TradingName(true,Some("foo")))

      val res = TradingNameForm.fillWithPrePop(prePopName,tradingNameModel)
      res.errors mustBe Seq.empty
      res mustBe testForm.bind(mappingOfForm).discardingErrors
    }
    "return a form populated with the model and the pre pop trading name as the model = Some and answer is false" in {
      val mappingOfForm = Map(
        "differentName" -> "false",
        "tradingName" -> "foo bar wizz pre pop"
      )
      val prePopName = Some("foo bar wizz pre pop")
      val tradingNameModel = Some(TradingName(false,None))

      val res = TradingNameForm.fillWithPrePop(prePopName,tradingNameModel)
      res.errors mustBe Seq.empty
      res mustBe testForm.bind(mappingOfForm).discardingErrors
    }
    "return a form with nothing populated as there is neither pre pop nor is there a model" in {
      val mappingOfForm = Map(
        "differentName" -> "",
        "tradingName" -> "")

      val res = TradingNameForm.fillWithPrePop(None,None)
      res.errors mustBe Seq.empty
      res mustBe testForm.bind(mappingOfForm).discardingErrors
    }
  }
}