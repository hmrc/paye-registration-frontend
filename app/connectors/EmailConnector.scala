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

package connectors

import config.AppConfig
import connectors.httpParsers.EmailHttpParsers
import models.external.EmailRequest
import models.{EmailDifficulties, EmailResponse}
import play.api.mvc.Request
import uk.gov.hmrc.http._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject()(val http: HttpClient, appConfig: AppConfig)(implicit val ec: ExecutionContext) extends EmailHttpParsers {

  val sendEmailURL: String = appConfig.servicesConfig.getString("microservice.services.email.sendAnEmailURL")

  def requestEmailToBeSent(emailRequest: EmailRequest)(implicit hc: HeaderCarrier, request: Request[_]): Future[EmailResponse] = {

    http.POST[EmailRequest, EmailResponse](sendEmailURL, emailRequest)(EmailRequest.format, requestEmailToBeSentHttpReads(emailRequest), hc, ec) recover {
      case e: Exception =>
        errorLog(s"[requestEmailToBeSent] " +
          s"an unexpected error has occurred when attemping to request an email to be sent via the email service " +
          s"with templateId: ${emailRequest.templateId} with details: ${e.getMessage}")
        EmailDifficulties
    }
  }
}