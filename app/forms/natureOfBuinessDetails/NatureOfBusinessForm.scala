/*
 * Copyright 2021 HM Revenue & Customs
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

package forms.natureOfBuinessDetails

import models.view.NatureOfBusiness
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms, Mapping}
import utils.Validators.isValidNatureOfBusiness

object NatureOfBusinessForm {
  def removeNewlineAndTrim(s: String): String = s.replaceAll("\r\n|\r|\n|\t", " ").trim

  def validate(entry: String): Either[Seq[FormError], String] = {
    removeNewlineAndTrim(entry) match {
      case t if t.length > 100 => Left(Seq(FormError("description", "errors.invalid.sic.overCharLimit")))
      case "" => Left(Seq(FormError("description", "errors.invalid.sic.noEntry")))
      case nob if isValidNatureOfBusiness(nob) => Right(nob)
      case _ => Left(Seq(FormError("description", "errors.invalid.sic.invalidChars")))
    }
  }

  implicit def natureOfBusinessFormatter: Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      validate(data.getOrElse(key, ""))
    }

    override def unbind(key: String, value: String): Map[String, String] = {
      Map("description" -> value)
    }
  }

  val natureOfBusiness: Mapping[String] = Forms.of[String](natureOfBusinessFormatter)

  val form = Form(
    mapping(
      "description" -> natureOfBusiness
    )(NatureOfBusiness.apply)(NatureOfBusiness.unapply)
  )
}
