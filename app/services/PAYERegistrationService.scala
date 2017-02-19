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
import javax.inject.{Inject, Singleton}

import enums.DownstreamOutcome
import connectors._
import models.BusinessContactDetails
import models.api.{CompanyDetails, Director, Employment, PAYERegistration => PAYERegistrationAPI}
import models.view.{Address, Summary, SummaryRow, SummarySection}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PAYERegistrationService @Inject()(keystoreConn: KeystoreConnector, payeRegistrationConn: PAYERegistrationConnector, s4LServ: S4LService) extends PAYERegistrationSrv {
  override val keystoreConnector = keystoreConn
  override val payeRegistrationConnector = payeRegistrationConn
  override val s4LService = s4LServ
}

trait PAYERegistrationSrv extends CommonService {

  val payeRegistrationConnector: PAYERegistrationConnect
  val s4LService: S4LSrv

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
        buildCompanyDetailsSection(apiModel.companyDetails),
        buildBusinessContactDetailsSection(apiModel.companyDetails.businessContactDetails),
        buildEmploymentSection(apiModel.employment),
        buildDirectorsSection(apiModel.directors)
      )
    )
  }

  private[services] def buildCompanyDetailsSection(companyDetails: CompanyDetails) : SummarySection = {
    SummarySection(
      id = "companyDetails",
      Seq(
        Some(SummaryRow(
          id ="tradingName",
          answer = companyDetails.tradingName match {
            case Some(tName) => Right(tName)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
        )),
        Some(SummaryRow(
          id = "roAddress",
          answer = Right(formatHTMLROAddress(companyDetails.roAddress)),
          changeLink = None
        ))
      ).flatten
    )
  }

  private[services] def buildBusinessContactDetailsSection(businessContactDetails: BusinessContactDetails) : SummarySection = {
    SummarySection(
      id = "businessContactDetails",
      Seq(
        Some(SummaryRow(
          id = "businessEmail",
          answer = businessContactDetails.businessEmail match {
            case Some(email) => Right(email)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        )),
        Some(SummaryRow(
          id = "mobileNumber",
          answer = businessContactDetails.mobileNumber match {
            case Some(mobile) => Right(mobile)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        )),
        Some(SummaryRow(
          id = "businessTelephone",
          answer = businessContactDetails.phoneNumber match {
            case Some(bizPhone) => Right(bizPhone)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        ))
      ).flatten
    )
  }

  private[services] def formatHTMLROAddress(address: Address): String = {
    List(
      Some(address.line1),
      Some(address.line2),
      address.line3,
      address.line4,
      address.postCode,
      address.country
    ).flatten.mkString("<br />")
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

  def buildDirectorsSection(directors: Seq[Director]) = {
    //TODO this needs to be updated to work properly. It also won't look correct without changing the current SummaryRow
    def directorRow(director: Director) = {
      SummaryRow(
        id = director.name.toString,
        answer = director.nino match {
          case Some(nino) => Left(nino)
          case None => Right("")
        },
        changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails())
      )
    }

    SummarySection(
      id = "directorDetails",
      for(d <- directors) yield directorRow(d)
    )
  }
}
