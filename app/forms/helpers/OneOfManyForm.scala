/*
 * Copyright 2023 HM Revenue & Customs
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

trait OneOfManyForm {

  val optionalFields: Seq[String]
  val noFieldsCompletedMessage: String

  implicit val multiPartFormatter = new Formatter[Option[String]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
      if (optionalFields.flatMap(data.get).forall(_ == "")) {
        Left(Seq(FormError(s"$key", noFieldsCompletedMessage)))
      } else {
        Right(data.get(key).filterNot(_ == ""))
      }
    }

    override def unbind(key: String, value: Option[String]): Map[String, String] = Map(key -> value.getOrElse(""))
  }

  val oneOfManyErrorTarget: Mapping[Option[String]] = Forms.of[Option[String]](multiPartFormatter)
}
