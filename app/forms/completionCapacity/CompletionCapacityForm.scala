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

package forms.completionCapacity

import enums.UserCapacity
import models.view.{CompletionCapacity => CompletionCapacityView}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError, Forms, Mapping}
import uk.gov.voa.play.form.ConditionalMappings._

import scala.util.{Success, Try}

object CompletionCapacityForm {

  private val ccRegex = """^[A-Za-z0-9 '\-]{1,100}$"""

  private def ifOther(mapping: Mapping[String]): Mapping[String] =
    onlyIf(isEqual("completionCapacity", "other"), mapping)("")

  implicit val completionCapacityFormatter = new Formatter[UserCapacity.Value] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], UserCapacity.Value] = {
      Try(UserCapacity.fromString(data.getOrElse(key, ""))) match {
        case Success(capacity) => Right(capacity)
        case _                 => Left(Seq(FormError(key, "pages.completionCapacity.error")))
      }
    }

    override def unbind(key: String, value: UserCapacity.Value): Map[String, String] = Map(key -> value.toString)
  }

  val completionCapacity: Mapping[UserCapacity.Value] = Forms.of[UserCapacity.Value](completionCapacityFormatter)

  val otherValidation: Mapping[String] = {
    val otherConstraint: Constraint[String] = Constraint("constraint.other")({ other =>
      val errors = if(other.trim.matches(ccRegex)) {
        Nil
      } else {
        other.trim match {
          case ""                         => Seq(ValidationError("pages.completionCapacity.other.label"))
          case long if long.length >= 100 => Seq(ValidationError("pages.completionCapacity.other.error"))
          case _                          => Seq(ValidationError("pages.completionCapacity.error.invalidChars"))
        }
      }
      if(errors.isEmpty) Valid else Invalid(errors)
    })
    text.verifying(otherConstraint)
  }

  val form = Form(
    mapping(
      "completionCapacity"      -> completionCapacity,
      "completionCapacityOther" -> ifOther(otherValidation)
    )(CompletionCapacityView.apply)(CompletionCapacityView.unapply)
  )
}
