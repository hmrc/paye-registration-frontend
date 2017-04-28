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

package forms.companyDetails

import common.exceptions.InternalExceptions
import forms.helpers.RequiredBooleanForm
import models.view.TradingName
import play.api.data.Form
import play.api.data.Forms._

object TradingNameForm extends RequiredBooleanForm {

  private val tradingNameRegex = """^[A-Za-z0-9\\\-,.()/&'!][A-Za-z 0-9\\\-,.()/&'!]{0,34}$"""

  def validateTradingName(vForm: Form[TradingName]): Boolean = {
    vForm.data("tradingName").trim.matches(tradingNameRegex)
}

  def validateForm(vForm: Form[TradingName]): Form[TradingName] = {
    if(!validationNeeded(vForm)) vForm else {
      if(tradingNameFieldNotCompleted(vForm)) {
        vForm.withError("tradingName", "pages.tradingName.errorQuestion")
      } else if(!validateTradingName(vForm)){
        vForm.withError("tradingName", "pages.tradingName.errorQuestion")
      } else {
        vForm
      }
    }
  }

  private def validationNeeded(data: Form[TradingName]): Boolean = {
    data("differentName").value.getOrElse{
      throw new InternalExceptions.ExpectedFormFieldNotPopulatedException("TradingNameForm", "differentName")
    } == "true"

//    data.data("differentName").trim match {
//      case "" => throw new InternalExceptions.ExpectedFormFieldNotPopulatedException("TradingNameForm", "differentName")
//    }
  }

  private def tradingNameFieldNotCompleted(data: Form[TradingName]) = data("tradingName").value.isEmpty

  val form = Form(
    mapping(
      "differentName" -> requiredBoolean,
      "tradingName" -> optional(text)
    )(TradingName.apply)(TradingName.unapply)
  )
}
