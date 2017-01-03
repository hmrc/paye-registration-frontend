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

import models.companyDetails.TradingNameModel
import enums.YesNo
import common.exceptions.InternalExceptions
import play.api.data.Form
import play.api.data.Forms._

object TradingNameForm {

  def validateForm(vForm: Form[TradingNameModel]): Form[TradingNameModel] = {
    if(!validationNeeded(vForm)) vForm else {
      if (tradingNameFieldNotCompleted(vForm)) vForm.withError("tradingName", "pages.tradingName.errorQuestion")
      else vForm
    }
  }

  private def validationNeeded(data: Form[TradingNameModel]): Boolean = {
    val yn = data("tradeUnderDifferentName").value.getOrElse(
      throw new InternalExceptions.ExpectedFormFieldNotPopulatedException("TradingNameForm", "tradeUnderDifferentName"))
    YesNo.fromString(yn) == YesNo.Yes
  }

  private def tradingNameFieldNotCompleted(data: Form[TradingNameModel]) = data("tradingName").value.isEmpty

  val form = Form(
    mapping(
      "tradeUnderDifferentName" -> text,
      "tradingName" -> optional(text)
    )(TradingNameModel.apply)(TradingNameModel.unapply)
  )
}
