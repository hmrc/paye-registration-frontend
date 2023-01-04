/*
 * Copyright 2023 HM Revenue & Customs
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

import ch.qos.logback.classic.Level
import connectors.ALFLocationHeaderNotSetException
import helpers.PayeComponentSpec
import models.{Address, EmailDifficulties, EmailSent}
import models.external.EmailRequest
import play.api.http.HeaderNames
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.LogCapturingHelper

class EmailHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

  "EmailHttpParsers" when {

    val emailRequest = EmailRequest(
      to = Seq("test@example.com"),
      templateId = "fooTemplate",
      parameters = Map("foo" -> "bar"),
      force = true
    )

    "calling .requestEmailToBeSentHttpReads" when {

      "response is 2xx and JSON is valid" must {

        "return EmailSent ADT and log an info messgae" in {

          withCaptureOfLoggingFrom(EmailHttpParsers.logger) { logs =>
            EmailHttpParsers.requestEmailToBeSentHttpReads(emailRequest).read("", "", HttpResponse(OK, "")) mustBe EmailSent
            logs.containsMsg(Level.INFO, s"[EmailHttpParsers][requestEmailToBeSent] Email has been sent successfully for template ${emailRequest.templateId}")
          }
        }
      }

      "response is any other status, e.g ISE" must {

        "return an EmailDifficulties ADT and log an error" in {

          withCaptureOfLoggingFrom(EmailHttpParsers.logger) { logs =>
            EmailHttpParsers.requestEmailToBeSentHttpReads(emailRequest).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe EmailDifficulties
            logs.containsMsg(Level.ERROR, s"[EmailHttpParsers][requestEmailToBeSent] an unexpected error has occurred when attemping to request an email to be sent via the email service with templateId: ${emailRequest.templateId} with status: '$INTERNAL_SERVER_ERROR'")
          }
        }
      }
    }
  }
}
