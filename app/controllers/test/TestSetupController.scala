/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import javax.inject.Inject
import connectors._
import connectors.test._
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Logger}
import services._
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ExecutionContext, Future}

class TestSetupControllerImpl @Inject()(val keystoreConnector: KeystoreConnector,
                                        val config: Configuration,
                                        val businessRegConnector: BusinessRegistrationConnector,
                                        val testBusinessRegConnector: TestBusinessRegConnector,
                                        val testIncorpInfoConnector: TestIncorpInfoConnector,
                                        val coHoAPIService: IncorporationInformationService,
                                        val messagesApi: MessagesApi,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val testPAYERegConnector: TestPAYERegConnector,
                                        val payeRegService: PAYERegistrationService,
                                        val authConnector: AuthConnector,
                                        val s4LService: S4LService,
                                        val incorporationInformationConnector: IncorporationInformationConnector,
                                        val payeRegistrationService: PAYERegistrationService) extends TestSetupController with AuthRedirectUrls

trait TestSetupController
  extends BusinessProfileController
    with TestCoHoController
    with TestRegSetupController
    with TestCacheController
    with PayeBaseController {

  val businessRegConnector: BusinessRegistrationConnector
  val testBusinessRegConnector: TestBusinessRegConnector
  val testIncorpInfoConnector: TestIncorpInfoConnector
  val coHoAPIService: IncorporationInformationService
  val payeRegService: PAYERegistrationService
  val testPAYERegConnector: TestPAYERegConnector
  val s4LService: S4LService

  private def log[T](f: String, res: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    res.flatMap (msg => {
      Logger.info(s"[TestSetupController] [$f] - ${msg.toString}")
      res
    })
  }

  def testSetup(companyName: String) = isAuthorised { implicit request =>
    for {
      bp <- log("CurrentProfileSetup", doBusinessProfileSetup)
      _  <- log("CoHoCompanyDetailsTeardown", doCoHoCompanyDetailsTearDown(bp.registrationID))
      _  <- log("AddCoHoCompanyDetails", doAddCoHoCompanyDetails(bp.registrationID, companyName))
      _  <- log("RegTeardown", doIndividualRegTeardown(bp.registrationID))
      _  <- log("S4LTeardown", doTearDownS4L(bp.registrationID))
      _  <- log("CCUpdate", testBusinessRegConnector.updateCompletionCapacity(bp.registrationID, "director"))
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
      resp    <- testIncorpInfoConnector.addIncorpUpdate(profile.registrationID, success, incorpDate, crn)
    } yield {
      Ok(s"Incorp Update added for regId: ${profile.registrationID} and success: $success")
    }).recover {
      case _ => InternalServerError(s"Unable to add Incorp Update")
    }
  }
}
