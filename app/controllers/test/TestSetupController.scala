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
import connectors.{BusinessRegistrationConnect, BusinessRegistrationConnector, KeystoreConnect, KeystoreConnector}
import connectors.test.{TestBusinessRegConnect, TestBusinessRegConnector, TestCoHoAPIConnect, TestCoHoAPIConnector, TestPAYERegConnect, TestPAYERegConnector}
import models.test.CoHoCompanyDetailsFormModel
import play.api.Logger
import play.api.i18n.MessagesApi
import services.{CoHoAPIService, CoHoAPISrv, PAYERegistrationService, PAYERegistrationSrv, S4LService, S4LSrv}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestSetupController @Inject()(injKeystoreConnector: KeystoreConnector,
                                    injBusinessRegConnector: BusinessRegistrationConnector,
                                    injTestBusinessRegConnector: TestBusinessRegConnector,
                                    injTestCoHoAPIConnector: TestCoHoAPIConnector,
                                    injCoHoAPIService: CoHoAPIService,
                                    injMessagesApi: MessagesApi,
                                    injTestPAYERegConnector: TestPAYERegConnector,
                                    injPayeRegService:PAYERegistrationService,
                                    injS4LService: S4LService) extends TestSetupCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val businessRegConnector = injBusinessRegConnector
  val testBusinessRegConnector = injTestBusinessRegConnector
  val testCoHoAPIConnector = injTestCoHoAPIConnector
  val coHoAPIService = injCoHoAPIService
  val messagesApi = injMessagesApi
  val payeRegService = injPayeRegService
  val testPAYERegConnector = injTestPAYERegConnector
  val s4LService = injS4LService
}

trait TestSetupCtrl extends CurrentProfileCtrl with TestCoHoCtrl with TestRegSetupCtrl with TestCacheCtrl {
  val keystoreConnector: KeystoreConnect
  val businessRegConnector: BusinessRegistrationConnect
  val testBusinessRegConnector: TestBusinessRegConnect
  val testCoHoAPIConnector: TestCoHoAPIConnect
  val coHoAPIService: CoHoAPISrv
  val payeRegService: PAYERegistrationSrv
  val testPAYERegConnector: TestPAYERegConnect
  val s4LService: S4LSrv

  private def log[T](f: String, res: Future[T]): Future[T] = {
    res.flatMap (msg => {
      Logger.info(s"[TestSetupController] [$f] - ${msg.toString}")
      res
    })
  }

  def testSetup(companyName: String) = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          _ <- log("CurrentProfileSetup", doCurrentProfileSetup)
          _ <- log("CoHoCompanyDetailsTeardown", doCoHoCompanyDetailsTearDown)
          _ <- log("AddCoHoCompanyDetails", doAddCoHoCompanyDetails(CoHoCompanyDetailsFormModel(companyName, List.empty, List.empty)))
          _ <- log("RegTeardown", doRegTeardown)
          _ <- log("S4LTeardown", doTearDownS4L)
        } yield Redirect(controllers.userJourney.routes.SignInOutController.postSignIn())
  }
}
