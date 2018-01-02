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

package forms.test

import models.api.PAYEContact
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.data.Form
import play.api.data.Forms._

object TestPAYEContactForm {

  val form = Form(
    mapping(
      "payeContactDetails" -> mapping(
        "name"                  -> nonEmptyText,
        "digitalContactDetails" -> mapping(
          "email"         -> optional(text),
          "mobileNumber"  -> optional(text),
          "phoneNumber"   -> optional(text)
        )(DigitalContactDetails.apply)(DigitalContactDetails.unapply)
      )(PAYEContactDetails.apply)(PAYEContactDetails.unapply),
      "correspondenceAddress" -> mapping(
        "line1"     -> text,
        "line2"     -> text,
        "line3"     -> optional(text),
        "line4"     -> optional(text),
        "postCode"  -> optional(text),
        "country"   -> optional(text),
        "auditRef"  -> optional(text)
      )(Address.apply)(Address.unapply)
    )(PAYEContact.apply)(PAYEContact.unapply)
  )
}
