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

package controllers.userJourney

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import utils.DateUtil
import views.html.pages.error.restart
import views.html.pages.{confirmation => ConfirmationPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val keystoreConnector: KeystoreConnector,
                                       val confirmationService: ConfirmationService,
                                       val s4LService: S4LService,
                                       val companyDetailsService: CompanyDetailsService,
                                       val incorpInfoService: IncorporationInformationService,
                                       val emailService: EmailService,
                                       val authConnector: AuthConnector,
                                       val incorporationInformationConnector: IncorporationInformationConnector,
                                       val payeRegistrationService: PAYERegistrationService,
                                       mcc: MessagesControllerComponents,
                                       restart: restart,
                                       ConfirmationPage: ConfirmationPage
                                      )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls with DateUtil {

  def showConfirmation: Action[AnyContent] = isAuthorisedWithProfileNoSubmissionCheck { implicit request =>
    profile => {
      for {
        Some(acknowledgementReference) <- confirmationService.getAcknowledgementReference(profile.registrationID)
        optFullName <- authConnector.authorise(EmptyPredicate,Retrievals.name).map(_.flatMap(_.name))
        _ <- emailService.sendAcknowledgementEmail(profile, acknowledgementReference, optFullName)
        _ <- s4LService.clear(profile.registrationID)
      } yield
        Ok(ConfirmationPage(
          acknowledgementReference,
          confirmationService.determineIfInclusiveContentIsShown,
          formatDate(confirmationService.endDate, "d MMMM")
        ))
    }.recover {
      case e =>
        InternalServerError(restart())
    }
  }
}
