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
import connectors.test._
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestSetupController @Inject()(
                                        val testIncorpInfoConnector: TestIncorpInfoConnector,
                                        val coHoAPIService: IncorporationInformationService,
                                        val testPAYERegConnector: TestPAYERegConnector,
                                        val payeRegService: PAYERegistrationService,
                                        val businessProfileController: BusinessProfileController,
                                        val appConfig: AppConfig,
                                        val testBusinessRegConnector: TestBusinessRegConnector,
                                        val authConnector: AuthConnector,
                                        val payeRegistrationService: PAYERegistrationService,
                                        val keystoreConnector: KeystoreConnector,
                                        val incorporationInformationConnector: IncorporationInformationConnector,
                                        val businessRegConnector: BusinessRegistrationConnector,
                                        val s4LService: S4LService,
                                        mcc: MessagesControllerComponents
                                   )(implicit val ec: ExecutionContext)
  extends PayeBaseController(mcc) with AuthRedirectUrls with Logging {

  private def log[T](f: String, res: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    res.flatMap(msg => {
      logger.info(s"[$f] ${msg.toString}")
      res
    })
  }

  def testSetup(companyName: String) = isAuthorised { implicit request =>
    for {
      bp <- log("CurrentProfileSetup", businessProfileController.doBusinessProfileSetup)
      _ <- log("CoHoCompanyDetailsTeardown", doCoHoCompanyDetailsTearDown(bp.registrationID))
      _ <- log("AddCoHoCompanyDetails", doAddCoHoCompanyDetails(bp.registrationID, companyName))
      _ <- log("RegTeardown", doIndividualRegTeardown(bp.registrationID))
      _ <- log("S4LTeardown", doTearDownS4L(bp.registrationID))
      _ <- log("CCUpdate", testBusinessRegConnector.updateCompletionCapacity(bp.registrationID, "director"))
    } yield Redirect(controllers.userJourney.routes.PayeStartController.steppingStone())
  }

  def updateStatus(status: String) = isAuthorised { implicit request =>
    businessRegConnector.retrieveCurrentProfile.flatMap { profile =>
      testPAYERegConnector.updateStatus(profile.registrationID, status) map {
        case DownstreamOutcome.Success => Ok(s"status for regId ${profile.registrationID} updated to '$status'")
        case DownstreamOutcome.Failure => InternalServerError(s"Unable to update status for regId ${profile.registrationID}")
      }
    }
  }

  def addIncorpUpdate(success: Boolean, incorpDate: Option[String], crn: Option[String]): Action[AnyContent] = isAuthorised { implicit request =>
    (for {
      profile <- businessRegConnector.retrieveCurrentProfile
      resp <- testIncorpInfoConnector.addIncorpUpdate(profile.registrationID, success, incorpDate, crn)
    } yield {
      Ok(s"Incorp Update added for regId: ${profile.registrationID} and success: $success")
    }).recover {
      case _ => InternalServerError(s"Unable to add Incorp Update")
    }
  }

  protected[controllers] def doCoHoCompanyDetailsTearDown(regId: String)(implicit request: Request[AnyContent]): Future[String] = {
    testIncorpInfoConnector.teardownIndividualCoHoCompanyDetails(regId).map(_ => "Company details collection removed")
  }

  protected[controllers] def doAddCoHoCompanyDetails(regId: String, companyName: String)(implicit request: Request[AnyContent]): Future[String] = {
    for {
      resp <- testIncorpInfoConnector.setupCoHoCompanyDetails(regId, companyName)
    } yield s"Company Name: $companyName, registration ID: $regId. Response status: ${resp.status}"
  }

  protected[controllers] def doIndividualRegTeardown(regId: String)(implicit request: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    testPAYERegConnector.tearDownIndividualRegistration(regId)
  }

  protected[controllers] def doTearDownS4L(regId: String)(implicit request: Request[AnyContent]): Future[String] = {
    s4LService.clear(regId: String) map (_ => "Save4Later cleared")
  }
}
