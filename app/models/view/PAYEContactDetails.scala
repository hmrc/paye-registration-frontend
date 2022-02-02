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

package models.view

import models.{Address, DigitalContactDetails}
import play.api.libs.json._
import utils.Formatters

case class PAYEContactDetails(name: String,
                              digitalContactDetails: DigitalContactDetails)

object PAYEContactDetails {
  implicit val digitalContactFormat = DigitalContactDetails.format
  implicit val format = Json.format[PAYEContactDetails]

  val prepopReads: Reads[PAYEContactDetails] = (json: JsValue) => {
    val oFirstName = (json \ "firstName").asOpt[String](Formatters.normalizeTrimmedReads)
    val oMiddleName = (json \ "middleName").asOpt[String](Formatters.normalizeTrimmedReads)
    val oLastName = (json \ "surname").asOpt[String](Formatters.normalizeTrimmedReads)

    DigitalContactDetails.prepopReads.reads(json) match {
      case jsSuccess: JsSuccess[DigitalContactDetails] => JsSuccess(
        PAYEContactDetails(
          name = Seq(oFirstName, oMiddleName, oLastName).flatten.mkString(" "),
          digitalContactDetails = jsSuccess.value)
      )
      case jsErr: JsError => jsErr
    }
  }

  val prepopWrites: Writes[PAYEContactDetails] = (payeContactDetails: PAYEContactDetails) => {
    def splitName(fullName: String): (Option[String], Option[String], Option[String]) = {
      val split = fullName.trim.split("\\s+")

      val firstName = if (fullName.trim.isEmpty) None else Some(split.head)
      val middleName = {
        val middleSplit = split
          .drop(1)
          .dropRight(1)
          .toList

        if (middleSplit.nonEmpty) Some(middleSplit.mkString(" ")) else None
      }
      val lastName = if (split.length < 2) None else Some(split.last)

      (firstName, middleName, lastName)
    }

    val (firstName, middleName, surname) = splitName(payeContactDetails.name)
    val jsonFirstName = firstName.fold(Json.obj())(fn => Json.obj("firstName" -> fn))
    val jsonMiddleName = middleName.fold(Json.obj())(mn => Json.obj("middleName" -> mn))
    val jsonSurname = surname.fold(Json.obj())(sn => Json.obj("surname" -> sn))

    jsonFirstName ++
      jsonMiddleName ++
      jsonSurname ++
      Json.toJson(payeContactDetails.digitalContactDetails)(DigitalContactDetails.prepopWrites).as[JsObject]
  }
}

case class PAYEContact(contactDetails: Option[PAYEContactDetails],
                       correspondenceAddress: Option[Address])

object PAYEContact {
  implicit val digitalContactFormat = DigitalContactDetails.format
  implicit val format = Json.format[PAYEContact]
}
