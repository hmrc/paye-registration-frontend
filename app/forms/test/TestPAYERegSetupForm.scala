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

package forms.test

import java.time.LocalDate

import enums.PAYEStatus
import forms.helpers.{DateForm, RequiredBooleanForm}
import models.{Address, DigitalContactDetails}
import models.api._
import models.view.PAYEContactDetails
import play.api.data.{Form, FormError, Forms, Mapping}
import play.api.data.Forms._
import play.api.data.format.Formatter

object TestPAYERegSetupForm extends RequiredBooleanForm with DateForm {

  override val prefix = "employment.firstPayDate"
  override def validation(dt: LocalDate) = Right(dt)

  implicit def payeStatusFormatter: Formatter[PAYEStatus.Value] = new Formatter[PAYEStatus.Value] {
    def bind(key: String, data: Map[String, String]) = {
      Right(data.getOrElse(key,"")).right.flatMap {
        case "draft" => Right(PAYEStatus.draft)
        case "" => Right(PAYEStatus.draft)
        case "held" => Right(PAYEStatus.held)
        case "submitted" => Right(PAYEStatus.submitted)
        case "invalid" => Right(PAYEStatus.invalid)
        case "rejected" => Right(PAYEStatus.rejected)
        case _ => Left(Seq(FormError(key, "error.required", Nil)))
      }
    }
    def unbind(key: String, value: PAYEStatus.Value) = Map(key -> value.toString)
  }

  val payeStatus: Mapping[PAYEStatus.Value] = Forms.of[PAYEStatus.Value](payeStatusFormatter)

  val form = Form(
    mapping(
      "registrationID" -> text,
      "formCreationTimestamp" -> text,
      "status" -> payeStatus,
      "completionCapacity" -> text,
      "companyDetails" -> mapping(
        "companyName" -> text,
        "tradingName" -> optional(text),
        "roAddress" -> mapping(
          "line1" -> text,
          "line2" -> text,
          "line3" -> optional(text),
          "line4" -> optional(text),
          "postCode" -> optional(text),
          "country" -> optional(text)
        )(Address.apply)(Address.unapply),
        "ppobAddress" -> mapping(
          "line1" -> text,
          "line2" -> text,
          "line3" -> optional(text),
          "line4" -> optional(text),
          "postCode" -> optional(text),
          "country" -> optional(text)
        )(Address.apply)(Address.unapply),
        "businessContactDetails" -> mapping(
          "businessEmail" -> optional(text),
          "mobileNumber" -> optional(text),
          "phoneNumber" -> optional(text)
        )(DigitalContactDetails.apply)(DigitalContactDetails.unapply)
      )(CompanyDetails.apply)(CompanyDetails.unapply),
      "employment" -> mapping(
        "employees" -> requiredBoolean,
        "companyPension" -> optional(requiredBoolean),
        "subcontractors" -> requiredBoolean,
        "firstPayDate" -> threePartDate
      )(Employment.apply)(Employment.unapply),
      "sicCodes" -> list(mapping(
        "code" -> optional(text),
        "description" -> optional(text)
      )(SICCode.apply)(SICCode.unapply)),
      "directors" -> list(mapping(
        "name" -> mapping(
          "firstName" -> optional(text),
          "middleName" -> optional(text),
          "lastName" -> optional(text),
          "title" -> optional(text)
        )(Name.apply)(Name.unapply),
        "nino" -> optional(text)
      )(Director.apply)(Director.unapply)),
      "payeContact" -> mapping(
        "payeContactDetails" -> mapping(
          "name" -> nonEmptyText,
          "digitalContactDetails" -> mapping(
            "email" -> optional(text),
            "mobileNumber" -> optional(text),
            "phoneNumber" -> optional(text)
          )(DigitalContactDetails.apply)(DigitalContactDetails.unapply)
        )(PAYEContactDetails.apply)(PAYEContactDetails.unapply),
        "correspondenceAddress" -> mapping(
          "line1" -> text,
          "line2" -> text,
          "line3" -> optional(text),
          "line4" -> optional(text),
          "postCode" -> optional(text),
          "country" -> optional(text)
        )(Address.apply)(Address.unapply)
      )(PAYEContact.apply)(PAYEContact.unapply)
    )(PAYERegistration.apply)(PAYERegistration.unapply)
  )
}
