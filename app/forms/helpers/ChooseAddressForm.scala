/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.helpers

import models.view.AddressChoice
import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}

trait ChooseAddressForm {

  val errMessage: String

  implicit def addressChoiceFormatter: Formatter[AddressChoice] = new Formatter[AddressChoice] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AddressChoice] = {
      Right(data.getOrElse(key, "")).right.flatMap {
        case "" => Left(Seq(FormError(key, errMessage, Nil)))
        case choice => Right(AddressChoice.fromString(choice))
      }
    }

    def unbind(key: String, value: AddressChoice): Map[String, String] = Map(key -> value.toString)
  }

  val chosenAddress: Mapping[AddressChoice] = Forms.of[AddressChoice](addressChoiceFormatter)
}
