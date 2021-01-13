/*
 * Copyright 2021 HM Revenue & Customs
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

import common.Logging
import config.{AppConfig, WSHttp}
import javax.inject.Inject
import models.external.EmailRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

sealed trait EmailResponse

case object EmailSent extends EmailResponse

case object EmailDifficulties extends EmailResponse

case object EmailNotFound extends EmailResponse

class EmailConnectorImpl @Inject()(val http: WSHttp, appConfig: AppConfig) extends EmailConnector {
  val sendEmailURL: String = appConfig.servicesConfig.getConfString("email.sendAnEmailURL",
    throw new Exception("email.sendAnEmailURL not found"))
}

trait EmailConnector extends Logging {
  val http: CorePost
  val sendEmailURL: String

  def requestEmailToBeSent(emailRequest: EmailRequest)(implicit hc: HeaderCarrier): Future[EmailResponse] = {
    http.POST[EmailRequest, HttpResponse](sendEmailURL, emailRequest).map { _ =>
      logger.info(s"Email has been sent successfully for template ${emailRequest.templateId}")
      EmailSent
    }.recover {
      case b: HttpException =>
        logger.warn(s"[requestEmailToBeSent] received a Http error when attempting to request an email to be sent via the email service with templateId: ${emailRequest.templateId} with details: ${b.getMessage}")
        EmailDifficulties
      case e: Exception =>
        logger.error(s"[requestEmailToBeSent] an unexpected error has occurred when attemping to request an email to be sent via the email service with templateId: ${emailRequest.templateId} with details: ${e.getMessage}")
        EmailDifficulties
    }
  }
}