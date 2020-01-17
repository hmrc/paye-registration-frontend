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

package forms.directorDetails

import models.view.{Ninos, UserEnteredNino}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms, Mapping}
import utils.Formatters
import utils.Validators.isValidNino

object DirectorDetailsForm {

  implicit def userNinoFormatter: Formatter[UserEnteredNino] = new Formatter[UserEnteredNino] {

    def getIndex(key: String): String = {
      key.filter("0123456789".toSet)
    }

    def bind(key: String, data: Map[String, String]) = {
      def emptyForm = if(getIndex(key) != "0") false else {
        data.map{
          case ("csrfToken", v) => ""
          case (k, v)           => v
        }.forall( _ == "")
      }

      def trimNino(nino: String): String = nino.replaceAll("\\s", "").toUpperCase

      def duplicates: Boolean = if(getIndex(key) != "0") false else {
        val ninoList = data.filter(tuple => isValidNino(trimNino(tuple._2))).toList map(ninoList => trimNino(ninoList._2))
        ninoList.size != ninoList.distinct.size
      }

      def nino = data.getOrElse(key,"")

      def showNinoError: Boolean = !(nino == "" || isValidNino(trimNino(nino)))

      (emptyForm, duplicates, showNinoError, nino) match {
        case (true, _, _, _)              => Left(Seq(FormError("noFieldsCompleted-nino[0]", "pages.directorDetails.errors.noneCompleted")))
        case (_, true, true, _)           => Left(Seq(FormError("", "errors.duplicate.nino"), FormError(key, "errors.invalid.nino")))
        case (_, true, false, _)          => Left(Seq(FormError("", "errors.duplicate.nino")))
        case (_, false, true, _)          => Left(Seq(FormError(key, "errors.invalid.nino")))
        case (_, false, false, "")        => Right(UserEnteredNino(getIndex(key), None))
        case (_, false, false, validNino) => Right(UserEnteredNino(getIndex(key), Some(trimNino(validNino))))
      }
    }

    def unbind(key: String, value: UserEnteredNino) = Map(s"nino[${value.id}]" -> Formatters.ninoFormatter(value.nino.getOrElse("")))
  }

  val userNino: Mapping[UserEnteredNino] = Forms.of[UserEnteredNino](userNinoFormatter)

  val form = Form(
    mapping(
      "nino" -> list(userNino)
    )(Ninos.apply)(Ninos.unapply)
  )
}
