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

package controllers.test

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors.test.{TestBusinessRegConnect, TestBusinessRegConnector}
import connectors._
import play.api.mvc.{AnyContent, Request}
import models.external.BusinessProfile
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class BusinessProfileController @Inject()(injKeystoreConnector: KeystoreConnector,
                                         injBusinessRegConnector: BusinessRegistrationConnector,
                                         injTestBusinessRegConnector: TestBusinessRegConnector)
  extends BusinessProfileCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val businessRegConnector = injBusinessRegConnector
  val testBusinessRegConnector = injTestBusinessRegConnector
}

trait BusinessProfileCtrl extends FrontendController with Actions {
  val keystoreConnector: KeystoreConnect
  val businessRegConnector: BusinessRegistrationConnect
  val testBusinessRegConnector: TestBusinessRegConnect

  def businessProfileSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          res <- doBusinessProfileSetup
        } yield Ok(res.toString)
  }

  protected[controllers] def doBusinessProfileSetup(implicit request: Request[AnyContent]): Future[BusinessProfile] = {
    businessRegConnector.retrieveCurrentProfile
      .recoverWith { case _ => testBusinessRegConnector.createBusinessProfileEntry }
  }
}
