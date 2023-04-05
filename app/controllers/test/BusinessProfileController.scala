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

package controllers.test

import config.AppConfig
import connectors._
import connectors.test.TestBusinessRegConnector
import controllers.{AuthRedirectUrls, PayeBaseController}
import models.external.BusinessProfile
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessProfileController @Inject()(val keystoreConnector: KeystoreConnector,
                                              val businessRegConnector: BusinessRegistrationConnector,
                                              val authConnector: AuthConnector,
                                              val s4LService: S4LService,
                                              val companyDetailsService: CompanyDetailsService,
                                              val incorpInfoService: IncorporationInformationService,
                                              val testBusinessRegConnector: TestBusinessRegConnector,
                                              val incorporationInformationConnector: IncorporationInformationConnector,
                                              val payeRegistrationService: PAYERegistrationService,
                                              mcc: MessagesControllerComponents
                                             )(val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  def businessProfileSetup = isAuthorised { implicit request =>
    doBusinessProfileSetup map { res =>
      Ok(res.toString)
    }
  }

  protected[controllers] def doBusinessProfileSetup(implicit request: Request[AnyContent]): Future[BusinessProfile] = {
    businessRegConnector.retrieveCurrentProfile
      .recoverWith { case _ => testBusinessRegConnector.createBusinessProfileEntry }
  }
}
