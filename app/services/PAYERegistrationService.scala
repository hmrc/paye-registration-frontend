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

package services

import java.time.format.DateTimeFormatter

import enums.DownstreamOutcome
import connectors._
import models.api.{Employment, PAYERegistration => PAYERegistrationAPI}
import models.view.{Summary, SummaryRow, SummarySection}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PAYERegistrationService extends PAYERegistrationService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val payeRegistrationConnector = PAYERegistrationConnector
  override val s4LService = S4LService
  //$COVERAGE-ON$
}

trait PAYERegistrationService extends CommonService {

  val payeRegistrationConnector: PAYERegistrationConnector
  val s4LService: S4LService

  def assertRegistrationFootprint()(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegistrationConnector.createNewRegistration(regID)
    } yield regResponse
  }

  def getRegistrationSummary()(implicit hc: HeaderCarrier): Future[Summary] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegistrationConnector.getRegistration(regID)
    } yield registrationToSummary(regResponse)
  }

  private[services] def registrationToSummary(apiModel: PAYERegistrationAPI): Summary = {
    Summary(
      Seq(
        SummarySection(
          id ="tradingName",
          Seq(
            SummaryRow(
              id ="tradingName",
              answer = apiModel.companyDetails.tradingName match {
                case Some(tName) => Right(tName)
                case _ => Left("noAnswerGiven")
              },
              changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
            )
          )
        ),
        buildEmploymentSection(apiModel.employment)
      )
    )
  }

  private[services] def buildEmploymentSection(employment : Employment) : SummarySection = {
    SummarySection(
      id = "employees",
      Seq(
        Some(SummaryRow(
          id = "employees",
          answer = employment.employees match {
            case true => Left("true")
            case false => Left("false")
          },
          changeLink = Some(controllers.userJourney.routes.EmploymentController.employingStaff())
        )),
        employment.companyPension map {
          ocpn =>
            SummaryRow(
              id = "companyPension",
              answer = ocpn match {
                case true => Left("true")
                case false => Left("false")
              },
              changeLink = Some(controllers.userJourney.routes.EmploymentController.companyPension())
            )
        },
        Some(SummaryRow(
          id = "subcontractors",
          answer = employment.subcontractors match {
            case true => Left("true")
            case false => Left("false")
          },
          changeLink = Some(controllers.userJourney.routes.EmploymentController.subcontractors())
        )),
        Some(SummaryRow(
          id = "firstPaymentDate",
          answer = Right(DateTimeFormatter.ofPattern("dd/MM/yyyy").format(employment.firstPayDate)),
          changeLink = Some(controllers.userJourney.routes.EmploymentController.firstPayment())
        ))
      ).flatten
    )
  }
}
