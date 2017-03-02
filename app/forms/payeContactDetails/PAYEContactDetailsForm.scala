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

package forms.payeContactDetails

import forms.helpers.OneOfManyForm
import models.DigitalContactDetails
import models.view.PAYEContactDetails
import play.api.data.Form
import play.api.data.Forms._
import utils.Validators._

object PAYEContactDetailsForm extends OneOfManyForm {

  override val optionalFields: Seq[String] = Seq("digitalContact.contactEmail", "digitalContact.mobileNumber", "digitalContact.phoneNumber")
  override val noFieldsCompletedMessage: String = "pages.payeContact.noFieldsCompleted"

  val form = Form(
    mapping(
    "name" -> text.verifying("pages.payeContact.nameMandatory", _.length > 0),
    "digitalContact" -> mapping(
        "contactEmail" -> oneOfManyErrorTarget.verifying(optionalValidation(emailValidation)),
        "mobileNumber" -> optional(text.verifying(mobilePhoneNumberValidation)),
        "phoneNumber" -> optional(text.verifying(phoneNumberValidation))
      )(DigitalContactDetails.apply)(DigitalContactDetails.unapply)
    )(PAYEContactDetails.apply)(PAYEContactDetails.unapply).verifying()
  )

}
