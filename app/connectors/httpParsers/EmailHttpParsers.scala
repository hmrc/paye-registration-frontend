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

package connectors.httpParsers

import models.external.EmailRequest
import models.{EmailDifficulties, EmailResponse, EmailSent}
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpReads, HttpResponse}
import utils.Logging

trait EmailHttpParsers extends Logging with HttpErrorFunctions {

  def requestEmailToBeSentHttpReads(emailRequest: EmailRequest): HttpReads[EmailResponse] = (_: String, _: String, response: HttpResponse) => response.status match {
    case status if is2xx(status) =>
      logger.info(s"[requestEmailToBeSent] Email has been sent successfully for template ${emailRequest.templateId}")
      EmailSent
    case status =>
      logger.error(s"[requestEmailToBeSent] an unexpected error has occurred when attemping to request an email to be sent via the email service with templateId: ${emailRequest.templateId} with status: '$status'")
      EmailDifficulties
  }
}

object EmailHttpParsers extends EmailHttpParsers
