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

package controllers.internal

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector, PAYERegistrationConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.{IncorporationStatus, RegistrationDeletion}
import play.api.libs.json.{JsObject, JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject()(val keystoreConnector: KeystoreConnector,
                                       val payeRegistrationConnector: PAYERegistrationConnector,
                                       val authConnector: AuthConnector,
                                       val companyDetailsService: CompanyDetailsService,
                                       val s4LService: S4LService,
                                       val incorpInfoService: IncorporationInformationService,
                                       val payeRegistrationService: PAYERegistrationService,
                                       val incorporationInformationConnector: IncorporationInformationConnector,
                                       mcc: MessagesControllerComponents
                                      )(val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {


  def delete(regId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised() {
      payeRegistrationService.deletePayeRegistrationInProgress(regId) map {
        case RegistrationDeletion.success => Ok
        case RegistrationDeletion.invalidStatus => PreconditionFailed
        case RegistrationDeletion.forbidden =>
          logger.warn(s"[delete] Requested document regId $regId to be deleted is not corresponding to the CurrentProfile regId")
          BadRequest
      } recover {
        case ex: Exception =>
          logger.error(s"[delete] Received an error when deleting Registration regId: $regId - error: ${ex.getMessage}")
          InternalServerError
      }
    } recover {
      case _ =>
        logger.warn(s"[delete] Can't get the Authority")
        Unauthorized
    }
  }

  def companyIncorporation: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val jsResp = request.body.as[JsObject]
    val txId = (jsResp \ "SCRSIncorpStatus" \ "IncorpSubscriptionKey" \ "transactionId").validate[String]
    val incorpStatus = (jsResp \ "SCRSIncorpStatus" \ "IncorpStatusEvent" \ "status").validate[IncorporationStatus.Value]

    (txId, incorpStatus) match {
      case (JsSuccess(id, _), JsSuccess(status, _)) => payeRegistrationService.handleIIResponse(id, status).map {
        s =>
          if (s == RegistrationDeletion.notfound) {
            logger.warn(s"[companyIncorporation] II returned $txId with status $incorpStatus but no paye doc found, returned 200 back to II to clear subscription")
          }
          Ok
      } recover {
        case e: Exception => InternalServerError
      }
      case _ =>
        logger.error(s"[companyIncorporation] Incorp Status or transaction Id not received or invalid from II for txId $txId")
        Future.successful(InternalServerError)
    }
  }
}