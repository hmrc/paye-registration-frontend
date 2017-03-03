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

package forms

import models.view.{AddressChoice, ChosenAddress}
import play.api.data.Forms.mapping
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms, Mapping}

object ChooseAddressForm {
  implicit def addressChoiceFormatter: Formatter[AddressChoice.Value] = new Formatter[AddressChoice.Value] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AddressChoice.Value] = {
      Right(data.getOrElse(key,"")).right.flatMap {
        case "" => Left(Seq(FormError(key, "errors.invalid.addressChoice.noEntry", Nil)))
        case choice => Right(AddressChoice.fromString(choice))
      }
    }

    def unbind(key: String, value: AddressChoice.Value): Map[String, String] = Map(key -> value.toString)
  }

  val chosenAddress: Mapping[AddressChoice.Value] = Forms.of[AddressChoice.Value](addressChoiceFormatter)

  val form = Form(
    mapping(
      "chosenAddress" -> chosenAddress
    )(ChosenAddress.apply)(ChosenAddress.unapply)
  )
}
