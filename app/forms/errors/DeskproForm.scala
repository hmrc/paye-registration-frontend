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

package forms.errors

import models.view.Ticket
import org.apache.commons.validator.routines.EmailValidator
import play.api.data.Form
import play.api.data.Forms._

private case class DeskproEmailValidator() {
  private val validator = EmailValidator.getInstance(false)
  def validate(email: String): Boolean = validator.isValid(email)
}

object DeskproForm {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val validateEmail: String => Boolean = emailValidator.validate

  val form = Form(
    mapping(
      "name" -> text
        .verifying("errorPages.failedSubmission.error.name_required", action => !action.trim.isEmpty)
        .verifying("errorPages.failedSubmission.error.name_too_long", name => name.size <= 70)
        .verifying("errorPages.failedSubmission.error.name_invalid_characters", name => name.matches( """^[A-Za-z\-.,()'"\s]+$""")),
      "email" -> text
        .verifying("errorPages.failedSubmission.error.email_format", validateEmail)
        .verifying("errorPages.failedSubmission.error.email_too_long", email => email.size <= 255),
      "message" -> text
        .verifying("errorPages.failedSubmission.error.message_required", action => !action.trim.isEmpty)
        .verifying("errorPages.failedSubmission.error.message_too_long", message => message.size <= 1000)
    )(Ticket.apply)(Ticket.unapply)
  )
}
