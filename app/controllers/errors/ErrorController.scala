/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.errors

import javax.inject.Inject
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.error.{ineligible => Ineligible, newIneligible => IneligiblePage, _}


import scala.concurrent.Future

class ErrorControllerImpl @Inject()(val messagesApi: MessagesApi,
                                    val config: Configuration,
                                    val thresholdService: ThresholdService,
                                    val keystoreConnector: KeystoreConnector,
                                    val companyDetailsService: CompanyDetailsService,
                                    val s4LService: S4LService,
                                    val incorpInfoService: IncorporationInformationService,
                                    val authConnector: AuthConnector,
                                    val incorporationInformationConnector: IncorporationInformationConnector,
                                    val payeRegistrationService: PAYERegistrationService) extends ErrorController with AuthRedirectUrls

trait ErrorController extends PayeBaseController {

  val thresholdService: ThresholdService

  def ineligible: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Ok(Ineligible()))
  }

  def newIneligible: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Ok(IneligiblePage(thresholdService.getCurrentThresholds.getOrElse("weekly", 116))))
  }

  def retrySubmission: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Ok(submissionTimeout()))
  }

  def failedSubmission: Action[AnyContent] =
    isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Ok(submissionFailed()))
  }
}
