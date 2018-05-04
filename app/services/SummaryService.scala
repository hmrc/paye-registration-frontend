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

package services

import java.time.format.DateTimeFormatter

import javax.inject.Inject
import common.exceptions.InternalExceptions.APIConversionException
import connectors._
import controllers.exceptions.{GeneralException, MissingSummaryBlockException, MissingSummaryBlockItemException}
import enums.UserCapacity
import models.api.{CompanyDetails, Director, Employment, EmploymentV2, PAYEContact, SICCode, PAYERegistration => PAYERegistrationAPI}
import models.view.{Summary, SummaryRow, SummarySection, EmployingStaffV2 => EmploymentView}
import models.{Address, DigitalContactDetails}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.{Formatters, PAYEFeatureSwitches}

import scala.concurrent.Future
import scala.util.{Success, Try}

class SummaryServiceImpl @Inject()(
                                    val payeRegistrationConnector: PAYERegistrationConnector,
                                    val employmentServiceV2: EmploymentServiceV2,
                                    val pAYEFeatureSwitches: PAYEFeatureSwitches
                                  ) extends SummaryService

trait SummaryService {
  val payeRegistrationConnector: PAYERegistrationConnector
  val employmentServiceV2: EmploymentServiceV2
  val pAYEFeatureSwitches: PAYEFeatureSwitches

  def getRegistrationSummary(regId: String)(implicit hc: HeaderCarrier): Future[Summary] = {
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield registrationToSummary(regResponse, regId)
  } recover {
    case e: Exception => throw GeneralException(s"[SummaryService][getRegistrationSummary] an error occured for regId $regId with message ${e.getMessage}")
  }

  private[services] def registrationToSummary(apiModel: PAYERegistrationAPI, regId: String): Summary = {
    Summary(Seq(
      if(pAYEFeatureSwitches.newApiStructure.enabled) {
        buildEmploymentV2Section(apiModel.employmentInfo, regId)
      }else {
        buildEmploymentSection(apiModel.employment, regId)
      },
      buildCompletionCapacitySection(apiModel.completionCapacity),
      buildCompanyDetailsSection(apiModel.companyDetails, apiModel.sicCodes),
      buildBusinessContactDetailsSection(apiModel.companyDetails.businessContactDetails),
      buildDirectorsSection(apiModel.directors),
      buildContactDetails(apiModel.payeContact)
    ))
  }

  private[services] def buildCompletionCapacitySection(capacity: String): SummarySection = {
    val displayCapacity = Try {
      UserCapacity.fromString(capacity)
    } match {
      case Success(UserCapacity.director)   => Left("director")
      case Success(UserCapacity.agent)      => Left("agent")
      case Success(UserCapacity.secretary)  => Left("companysecretary")
      case _                                => Right(capacity)
    }
    SummarySection(
      id = "completionCapacity",
      Seq(
        SummaryRow(
          id = "completionCapacity",
          answers = List(displayCapacity),
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
          answers = List(companyDetails.tradingName match {
            case Some(tName) => Right(tName)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
        ),
        SummaryRow(
          id = "roAddress",
          answers = addressToSummaryRowAnswers(companyDetails.roAddress),
          changeLink = None
        ),
        SummaryRow(
          id = "ppobAddress",
          answers = addressToSummaryRowAnswers(companyDetails.ppobAddress),
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress())
        ),
        SummaryRow(
          id = "natureOfBusiness",
          answers = List(Right(sicCodes.head.description.getOrElse{throw new APIConversionException("No nature of business provided for summary")})),
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
          answers = List(businessContactDetails.email match {
            case Some(email) => Right(email)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        ),
        SummaryRow(
          id = "mobileNumber",
          answers = List(businessContactDetails.mobileNumber match {
            case Some(mobile) => Right(mobile)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        ),
        SummaryRow(
          id = "businessTelephone",
          answers = List(businessContactDetails.phoneNumber match {
            case Some(bizPhone) => Right(bizPhone)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
        )
      )
    )
  }

  private[services] def addressToSummaryRowAnswers(address: Address): List[Either[String, String]] = {
    List(Right(address.line1), Right(address.line2)) ::: List(address.line3,
                                                              address.line4,
                                                              address.postCode,
                                                              address.country).flatten.map(Right(_))
  }

  private[services] def buildEmploymentV2Section(oEmployment: Option[EmploymentV2], regId: String): SummarySection = {
    val employment        = oEmployment.getOrElse(throw MissingSummaryBlockException(block = "EmployingStaffV2", regId))
    val employmentView    = employmentServiceV2.apiToView(employment)
    val cis               = employmentView.construction.getOrElse(throw MissingSummaryBlockItemException(block = "EmploymentStaffV2", item = "construction", regId))

    val employingAnyoneSection = employmentView.employingAnyone.map { paidEmployees =>
      Seq(
        Some(SummaryRow(
          id = "employing",
          answers = List(paidEmployees.employing match {
            case true => Left("true")
            case false => Left("false")
          }),
          changeLink = Some(controllers.userJourney.routes.NewEmploymentController.paidEmployees())
        )), paidEmployees.startDate.map {
          date =>
            SummaryRow(
              id = "earliestDate",
              answers = List(Right(DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date))),
              changeLink = Some(controllers.userJourney.routes.NewEmploymentController.paidEmployees())
            )
        }
      ).flatten
    }.toSeq.flatten

    val willBePayingSection = employmentView.willBePaying.map{ wbp =>
      Seq(
        Some(SummaryRow(
          id = "",
          answers = List(Left(wbp.willPay.toString)),
          changeLink = Some(controllers.userJourney.routes.NewEmploymentController.employingStaff())
        )),
        wbp.beforeSixApril.map {
          bsa =>
            SummaryRow(
              id = "beforeNextTaxYear",
              answers = List(Left(bsa.toString)),
              changeLink = Some(controllers.userJourney.routes.NewEmploymentController.employingStaff())
            )
        }
      ).flatten
    }.toSeq.flatten

    val cisSection = Seq(SummaryRow(
      id = "inConstructionIndustry",
      answers = List(Left(cis.toString)),
      changeLink = Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry())
    ))

    val subContractorsSection = employmentView.subcontractors.map{ sc =>
      SummaryRow(
        id = "employsSubcontractors",
        answers = List(Left(sc.toString)),
        changeLink = Some(controllers.userJourney.routes.NewEmploymentController.subcontractors())
      )
    }.toSeq

    val pensionsSection = employmentView.companyPension.map{ cp =>
      SummaryRow(
        id = "paysPension",
        answers = List(Left(cp.toString)),
        changeLink = Some(controllers.userJourney.routes.NewEmploymentController.pensions())
      )
    }.toSeq

    SummarySection(
      id = "employees",
      employingAnyoneSection ++ willBePayingSection ++ cisSection ++ subContractorsSection ++ pensionsSection
    )
  }

  private[services] def buildEmploymentSection(oEmployment : Option[Employment], regId: String) : SummarySection = {

    val employment = oEmployment.getOrElse(throw MissingSummaryBlockException(block = "Employment", regId))

    SummarySection(
      id = "employees",
      Seq(
        Some(SummaryRow(
          id = "employees",
          answers = List(employment.employees match {
            case true => Left("true")
            case false => Left("false")
          }),
          changeLink = Some(controllers.userJourney.routes.EmploymentController.employingStaff())
        )),
        employment.companyPension map {
          ocpn =>
            SummaryRow(
              id = "companyPension",
              answers = List(ocpn match {
                case true => Left("true")
                case false => Left("false")
              }),
              changeLink = Some(controllers.userJourney.routes.EmploymentController.companyPension())
            )
        },
        Some(SummaryRow(
          id = "subcontractors",
          answers = List(employment.subcontractors match {
            case true => Left("true")
            case false => Left("false")
          }),
          changeLink = Some(controllers.userJourney.routes.EmploymentController.subcontractors())
        )),
        Some(SummaryRow(
          id = "firstPaymentDate",
          answers = List(Right(DateTimeFormatter.ofPattern("dd/MM/yyyy").format(employment.firstPayDate))),
          changeLink = Some(controllers.userJourney.routes.EmploymentController.firstPayment())
        ))
      ).flatten
    )
  }

  private[services] def buildDirectorsSection(directors: Seq[Director]) = {
    def directorRow(director: Director, i: Int) = {
      SummaryRow(
        id = "director" + i,
        answers = List(director.nino match {
          case Some(nino) => Right(Formatters.ninoFormatter(nino))
          case None => Right("")
        }),
        changeLink = Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails()),
        questionArgs = Some(Seq(List(director.name.forename, Some(director.name.surname)).flatten.mkString(" "))),
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
          answers = List(Right(payeContactDetails.contactDetails.name)),
          changeLink = changeCall
        ),
        SummaryRow(
          id = "emailPAYEContact",
          answers = List(digitalContact.email match {
            case Some(email) => Right(email)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = changeCall
        ),
        SummaryRow(
          id = "mobileNumberPAYEContact",
          answers = List(digitalContact.mobileNumber match {
            case Some(mobile) => Right(mobile)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = changeCall
        ),
        SummaryRow(
          id = "phoneNumberPAYEContact",
          answers = List(digitalContact.phoneNumber match {
            case Some(phone) => Right(phone)
            case _ => Left("noAnswerGiven")
          }),
          changeLink = changeCall
        ),
        SummaryRow(
          id = "correspondenceAddress",
          answers = addressToSummaryRowAnswers(payeContactDetails.correspondenceAddress),
          changeLink = Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress())
        )
      )
    )
  }
}
