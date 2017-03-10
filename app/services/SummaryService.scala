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

import common.exceptions.InternalExceptions.APIConversionException
import connectors._
import enums.UserCapacity
import models.api.{CompanyDetails, Director, Employment, PAYEContact, SICCode, PAYERegistration => PAYERegistrationAPI}
import models.view.{Summary, SummaryRow, SummarySection}
import models.{Address, DigitalContactDetails}
import play.api.mvc.Call
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

@Singleton
class SummaryService @Inject()(payeRegistrationConn: PAYERegistrationConnector) extends SummarySrv {
  override val payeRegistrationConnector = payeRegistrationConn
}

trait SummarySrv {

  val payeRegistrationConnector: PAYERegistrationConnect

  def getRegistrationSummary(regId: String)(implicit hc: HeaderCarrier): Future[Summary] = {
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield registrationToSummary(regResponse)
  }

  private[services] def registrationToSummary(apiModel: PAYERegistrationAPI): Summary = {
    Summary(
      Seq(
        buildCompletionCapacitySection(apiModel.completionCapacity),
        buildCompanyDetailsSection(apiModel.companyDetails, apiModel.sicCodes),
        buildBusinessContactDetailsSection(apiModel.companyDetails.businessContactDetails),
        buildEmploymentSection(apiModel.employment),
        buildDirectorsSection(apiModel.directors),
        buildContactDetails(apiModel.payeContact)
      )
    )
  }

  private[services] def buildCompletionCapacitySection(capacity: String): SummarySection = {
    val displayCapacity = Try {
      UserCapacity.fromString(capacity)
    } match {
      case Success(UserCapacity.director) => Left("director")
      case Success(UserCapacity.agent)    => Left("agent")
      case _ => Right(capacity)
    }
    SummarySection(
      id = "completionCapacity",
      Seq(
        SummaryRow(
          id = "completionCapacity",
          displayCapacity,
          changeLink = Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
        )
      )
    )
  }

  private[services] def buildCompanyDetailsSection(companyDetails: CompanyDetails, sicCodes: List[SICCode]) : SummarySection = {
    SummarySection(
      id = "companyDetails",
      Seq(
        SummaryRow(
          id ="tradingName",
          answer = companyDetails.tradingName match {
            case Some(tName) => Right(tName)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
        ),
        SummaryRow(
          id = "roAddress",
          answer = Right(formatHTMLAddress(companyDetails.roAddress)),
          changeLink = None
        ),
        SummaryRow(
          id = "ppobAddress",
          answer = Right(formatHTMLAddress(companyDetails.ppobAddress)),
          //TODO: Change to PPOB address route
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress())
        ),
        SummaryRow(
          id = "natureOfBusiness",
          answer = Right(sicCodes.head.description.getOrElse{throw new APIConversionException("No nature of business provided for summary")}),
          changeLink = Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness())
        )
      )
    )
  }

  private[services] def buildBusinessContactDetailsSection(businessContactDetails: DigitalContactDetails) : SummarySection = {
    SummarySection(
      id = "businessContactDetails",
      Seq(
        SummaryRow(
          id = "businessEmail",
          answer = businessContactDetails.email match {
            case Some(email) => Right(email)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        ),
        SummaryRow(
          id = "mobileNumber",
          answer = businessContactDetails.mobileNumber match {
            case Some(mobile) => Right(mobile)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        ),
        SummaryRow(
          id = "businessTelephone",
          answer = businessContactDetails.phoneNumber match {
            case Some(bizPhone) => Right(bizPhone)
            case _ => Left("noAnswerGiven")
          },
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        )
      )
    )
  }

  private[services] def formatHTMLAddress(address: Address): String = {
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

  private[services] def buildDirectorsSection(directors: Seq[Director]) = {
    def directorRow(director: Director, i: Int) = {
      SummaryRow(
        id = "director" + i,
        answer = director.nino match {
          case Some(nino) => Right(nino)
          case None => Right("")
        },
        changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
        questionArgs = Some(Seq(List(director.name.forename, director.name.surname).flatten.mkString(" "))),
        commonQuestionKey = Some("director")
      )
    }

    SummarySection(
      id = "directorDetails",
      for(d <- directors.zipWithIndex) yield directorRow(d._1, d._2)
    )
  }

  private[services] def buildContactDetails(payeContactDetails: PAYEContact) = {
    val changeCall: Option[Call] = Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails())
    val digitalContact = payeContactDetails.contactDetails.digitalContactDetails
    SummarySection(
      id = "payeContactDetails",
      Seq(
        SummaryRow(
          id = "contactName",
          answer = Right(payeContactDetails.contactDetails.name),
          changeLink = changeCall
        ),
        SummaryRow(
          id = "emailPAYEContact",
          answer = digitalContact.email match {
            case Some(email) => Right(email)
            case _ => Left("noAnswerGiven")
          },
          changeLink = changeCall
        ),
        SummaryRow(
          id = "mobileNumberPAYEContact",
          answer = digitalContact.mobileNumber match {
            case Some(mobile) => Right(mobile)
            case _ => Left("noAnswerGiven")
          },
          changeLink = changeCall
        ),
        SummaryRow(
          id = "phoneNumberPAYEContact",
          answer = digitalContact.phoneNumber match {
            case Some(phone) => Right(phone)
            case _ => Left("noAnswerGiven")
          },
          changeLink = changeCall
        ),
        SummaryRow(
          id = "correspondenceAddress",
          answer = Right(formatHTMLAddress(payeContactDetails.correspondenceAddress)),
          changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress())
        )
      )
    )
  }
}
