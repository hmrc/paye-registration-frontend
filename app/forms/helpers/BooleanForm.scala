/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}

trait BooleanForm {

  implicit def requiredBooleanFormatter(errorMsg: String): Formatter[Boolean] = new Formatter[Boolean] {

    override val format = Some(("format.boolean", Nil))

    // default play binding is to data.getOrElse(key, "false")
    def bind(key: String, data: Map[String, String]) = {
      Right(data.getOrElse(key, "")).right.flatMap {
        case "true" => Right(true)
        case "false" => Right(false)
        case _ => Left(Seq(FormError(key, errorMsg, Nil)))
      }
    }

    def unbind(key: String, value: Boolean) = Map(key -> value.toString)
  }

  val requiredBoolean: String => Mapping[Boolean] = errorMsg => Forms.of[Boolean](requiredBooleanFormatter(errorMsg))

}
