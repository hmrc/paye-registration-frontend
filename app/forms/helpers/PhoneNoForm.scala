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

package forms.helpers

import play.api.data.{FormError, Forms, Mapping}
import play.api.data.format.Formatter
import utils.Validators.isValidPhoneNo

trait PhoneNoForm {
  implicit def phoneNoFormatter(errMsg: String): Formatter[Option[String]] = new Formatter[Option[String]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
      val input = data.getOrElse(key, "")

      if (input.isEmpty) Right(None) else {
        isValidPhoneNo(input, errMsg) match {
          case Right(phoneNo) => Right(Some(phoneNo))
          case Left(err) => Left(Seq(FormError(key, err)))
        }
      }
    }

    override def unbind(key: String, value: Option[String]): Map[String, String] = Map(key -> value.getOrElse(""))
  }

  def phoneNoField(errMsg: String): Mapping[Option[String]] = Forms.of[Option[String]](phoneNoFormatter(errMsg))
}
