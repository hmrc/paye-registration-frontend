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
import connectors._
import connectors.test._
import enums.DownstreamOutcome
import models.test.CoHoCompanyDetailsFormModel
import play.api.Logger
import play.api.i18n.MessagesApi
import services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestSetupController @Inject()(injKeystoreConnector: KeystoreConnector,
                                    injBusinessRegConnector: BusinessRegistrationConnector,
                                    injTestBusinessRegConnector: TestBusinessRegConnector,
                                    injTestIncorpInfoConnector: TestIncorpInfoConnector,
                                    injCoHoAPIService: IncorporationInformationService,
                                    injMessagesApi: MessagesApi,
                                    injTestPAYERegConnector: TestPAYERegConnector,
                                    injPayeRegService:PAYERegistrationService,
                                    injPayeRegistrationConnector: PAYERegistrationConnector,
                                    injS4LService: S4LService) extends TestSetupCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val businessRegConnector = injBusinessRegConnector
  val testBusinessRegConnector = injTestBusinessRegConnector
  val testIncorpInfoConnector = injTestIncorpInfoConnector
  val coHoAPIService = injCoHoAPIService
  val messagesApi = injMessagesApi
  val payeRegService = injPayeRegService
  val testPAYERegConnector = injTestPAYERegConnector
  val s4LService = injS4LService
  val payeRegistrationConnector = injPayeRegistrationConnector
}

trait TestSetupCtrl extends BusinessProfileCtrl with TestCoHoCtrl with TestRegSetupCtrl with TestCacheCtrl {
  val keystoreConnector: KeystoreConnect
  val businessRegConnector: BusinessRegistrationConnect
  val testBusinessRegConnector: TestBusinessRegConnect
  val testIncorpInfoConnector: TestIncorpInfoConnect
  val coHoAPIService: IncorporationInformationSrv
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
            businessProfile <- log("CurrentProfileSetup", doBusinessProfileSetup)
            _ <- log("CoHoCompanyDetailsTeardown", doCoHoCompanyDetailsTearDown(businessProfile.registrationID))
            _ <- log("AddCoHoCompanyDetails", doAddCoHoCompanyDetails(businessProfile.registrationID, companyName))
            _ <- log("RegTeardown", doIndividualRegTeardown(businessProfile.registrationID))
            _ <- log("S4LTeardown", doTearDownS4L(businessProfile.registrationID))
          } yield Redirect(controllers.userJourney.routes.PayeStartController.startPaye())

  }

  def updateStatus(status: String) = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        businessRegConnector.retrieveCurrentProfile.flatMap { profile =>
          testPAYERegConnector.updateStatus(profile.registrationID, status) map {
            case DownstreamOutcome.Success => Ok(s"status for regId ${profile.registrationID} updated to '$status'")
            case DownstreamOutcome.Failure => InternalServerError(s"Unable to update status for regId ${profile.registrationID}")
          }
        }
  }
}
