/*
 * Copyright 2021 HM Revenue & Customs
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

import common.exceptions.InternalExceptions.APIConversionException
import connectors._
import enums.UserCapacity
import models.api.{CompanyDetails, Director, Employment, PAYEContact, SICCode, PAYERegistration => PAYERegistrationAPI}
import models.view.{Summary, SummaryChangeLink, SummaryRow, SummarySection}
import models.{Address, DigitalContactDetails}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Call, Request}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.Formatters

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class SummaryService @Inject()(val payeRegistrationConnector: PAYERegistrationConnector,
                               val employmentService: EmploymentService,
                               val iiService: IncorporationInformationService,
                               val messagesApi: MessagesApi
                              )(implicit ec: ExecutionContext) {
  def getRegistrationSummary(regId: String, txId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Summary] = {
    implicit val messages: Messages = messagesApi.preferred(request)

    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
      incorporationDate <- iiService.getIncorporationDate(regId, txId)
    } yield registrationToSummary(regResponse, regId, incorporationDate)
  }

  private[services] def registrationToSummary(apiModel: PAYERegistrationAPI, regId: String, incorporationDate: Option[LocalDate])(implicit messages: Messages): Summary =
    Summary(Seq(
      buildEmploymentSectionFromView(apiModel.employmentInfo, regId, incorporationDate),
      buildCompletionCapacitySection(apiModel.completionCapacity),
      buildCompanyDetailsSection(apiModel.companyDetails, apiModel.sicCodes),
      buildBusinessContactDetailsSection(apiModel.companyDetails.businessContactDetails),
      buildDirectorsSection(apiModel.directors),
      buildContactDetails(apiModel.payeContact)
    ))

  private[services] def buildCompletionCapacitySection(capacity: String)(implicit messages: Messages): SummarySection = {
    val displayCapacity: String = Try {
      UserCapacity.fromString(capacity)
    } match {
      case Success(UserCapacity.director) => messages("pages.summary.completionCapacity.answers.director")
      case Success(UserCapacity.agent) => messages("pages.summary.completionCapacity.answers.agent")
      case Success(UserCapacity.secretary) => messages("pages.summary.completionCapacity.answers.companysecretary")
      case _ => capacity
    }

    SummarySection(
      id = "completionCapacity",
      sectionHeading = messages("pages.summary.completionCapacity.sectionHeading"),
      Seq(
        SummaryRow(
          id = "completionCapacity",
          question = messages("pages.summary.completionCapacity.question"),
          answers = Seq(displayCapacity),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.CompletionCapacityController.completionCapacity,
              messages("pages.summary.completionCapacity.hiddenChangeText")
            )
          )
        )
      )
    )
  }

  private[services] def buildCompanyDetailsSection(companyDetails: CompanyDetails, sicCodes: List[SICCode])(implicit messages: Messages): SummarySection = {
    SummarySection(
      id = "companyDetails",
      sectionHeading = messages("pages.summary.companyDetails.sectionHeading"),
      Seq(
        SummaryRow(
          id = "tradingName",
          question = messages("pages.summary.tradingName.question"),
          answers = List(companyDetails.tradingName match {
            case Some(tName) => tName
            case _ => messages("pages.summary.tradingName.answers.noAnswerGiven")
          }),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.CompanyDetailsController.tradingName,
              messages("pages.summary.tradingName.hiddenChangeText")
            )
          )
        ),
        SummaryRow(
          id = "roAddress",
          question = messages("pages.summary.roAddress.question"),
          answers = addressToSummaryRowAnswers(companyDetails.roAddress),
          optChangeLink = None
        ),
        SummaryRow(
          id = "ppobAddress",
          question = messages("pages.summary.ppobAddress.question"),
          answers = addressToSummaryRowAnswers(companyDetails.ppobAddress),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.CompanyDetailsController.ppobAddress,
              messages("pages.summary.ppobAddress.hiddenChangeText")
            )
          )
        ),
        SummaryRow(
          id = "natureOfBusiness",
          question = messages("pages.summary.natureOfBusiness.question"),
          answers = List(sicCodes.head.description.getOrElse {
            throw new APIConversionException("No nature of business provided for summary")
          }),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness,
              messages("pages.summary.natureOfBusiness.hiddenChangeText")
            )
          )
        )
      )
    )
  }

  private[services] def buildBusinessContactDetailsSection(businessContactDetails: DigitalContactDetails)(implicit messages: Messages): SummarySection = {
    SummarySection(
      id = "businessContactDetails",
      sectionHeading = messages("pages.summary.businessContactDetails.sectionHeading"),
      Seq(
        SummaryRow(
          id = "businessEmail",
          question = messages("pages.summary.businessEmail.question"),
          answers = List(businessContactDetails.email match {
            case Some(email) => email
            case _ => messages("pages.summary.businessEmail.answers.noAnswerGiven")
          }),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.CompanyDetailsController.businessContactDetails,
              messages("pages.summary.businessEmail.hiddenChangeText")
            )
          )
        ),
        SummaryRow(
          id = "mobileNumber",
          question = messages("pages.summary.mobileNumber.question"),
          answers = List(businessContactDetails.mobileNumber match {
            case Some(mobile) => mobile
            case _ => messages("pages.summary.mobileNumber.answers.noAnswerGiven")
          }),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.CompanyDetailsController.businessContactDetails,
              messages("pages.summary.mobileNumber.hiddenChangeText")
            )
          )
        ),
        SummaryRow(
          id = "businessTelephone",
          question = messages("pages.summary.businessTelephone.question"),
          answers = List(businessContactDetails.phoneNumber match {
            case Some(businessPhoneNumber) => businessPhoneNumber
            case _ => messages("pages.summary.businessTelephone.answers.noAnswerGiven")
          }),
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.CompanyDetailsController.businessContactDetails,
              messages("pages.summary.businessTelephone.hiddenChangeText")
            )
          )
        )
      )
    )
  }

  private[services] def addressToSummaryRowAnswers(address: Address): Seq[String] =
    Seq(
      Some(address.line1),
      Some(address.line2),
      address.line3,
      address.line4,
      address.postCode,
      address.country
    ).flatten

  private[services] def buildEmploymentSectionFromView(oEmployment: Employment, regId: String, incorpDate: Option[LocalDate])(implicit messages: Messages): SummarySection = {
    val employmentView = employmentService.apiToView(oEmployment, incorpDate)

    val inConstructionIndustry = employmentView.construction.getOrElse(throw new InternalServerException(s"Element 'construction' was missing from 'EmploymentStaffV2' while building summary for regId: $regId"))

    val employingAnyoneSection = employmentView.employingAnyone.map { paidEmployees =>
      Seq(
        Some(SummaryRow(
          id = "employing",
          question = messages("pages.summary.employing.question"),
          answers = if (paidEmployees.employing) {
            Seq(messages("pages.summary.employing.answers.true"))
          } else {
            Seq(messages("pages.summary.employing.answers.false"))
          },
          optChangeLink = Some(
            SummaryChangeLink(
              controllers.userJourney.routes.EmploymentController.paidEmployees,
              messages("pages.summary.employing.hiddenChangeText")
            )
          )
        )),
        paidEmployees.startDate.map {
          startDate =>
            SummaryRow(
              id = "earliestDate",
              question = messages("pages.summary.earliestDate.question"),
              answers = Seq(DateTimeFormatter.ofPattern("dd/MM/yyyy").format(startDate)),
              optChangeLink = Some(SummaryChangeLink(
                controllers.userJourney.routes.EmploymentController.paidEmployees,
                messages("pages.summary.earliestDate.hiddenChangeText")
              ))
            )
        }
      ).flatten
    }.toSeq.flatten

    val willBePayingSection = employmentView.willBePaying.map { willBePaying =>
      Seq(
        Some(SummaryRow(
          id = "willBePaying",
          question = messages("pages.summary.willBePaying.question"),
          answers = if (willBePaying.willPay) {
            Seq(messages("pages.summary.willBePaying.answers.true"))
          } else {
            Seq(messages("pages.summary.willBePaying.answers.false"))
          },
          optChangeLink = Some(SummaryChangeLink(
            controllers.userJourney.routes.EmploymentController.employingStaff,
            messages("pages.summary.willBePaying.hiddenChangeText")
          ))
        )),
        willBePaying.beforeSixApril.map {
          beforeNextTaxYear =>
            SummaryRow(
              id = "beforeNextTaxYear",
              question = messages("pages.summary.beforeNextTaxYear.question"),
              answers = if (beforeNextTaxYear) {
                Seq(messages("pages.summary.beforeNextTaxYear.answers.true"))
              } else {
                Seq(messages("pages.summary.beforeNextTaxYear.answers.false"))
              },
              optChangeLink = Some(SummaryChangeLink(
                controllers.userJourney.routes.EmploymentController.employingStaff,
                messages("pages.summary.beforeNextTaxYear.hiddenChangeText")
              ))
            )
        }
      ).flatten
    }.toSeq.flatten

    val cisSection = Seq(SummaryRow(
      id = "inConstructionIndustry",
      question = messages("pages.summary.inConstructionIndustry.question"),
      answers = if (inConstructionIndustry) {
        Seq(messages("pages.summary.inConstructionIndustry.answers.true"))
      } else {
        Seq(messages("pages.summary.inConstructionIndustry.answers.false"))
      },
      optChangeLink = Some(SummaryChangeLink(
        controllers.userJourney.routes.EmploymentController.constructionIndustry,
        messages("pages.summary.inConstructionIndustry.hiddenChangeText")
      ))
    ))

    val subContractorsSection = employmentView.subcontractors.map { employsSubcontractors =>
      SummaryRow(
        id = "employsSubcontractors",
        question = messages("pages.summary.employsSubcontractors.question"),
        answers = if (employsSubcontractors) {
          Seq(messages("pages.summary.employsSubcontractors.answers.true"))
        } else {
          Seq(messages("pages.summary.employsSubcontractors.answers.false"))
        }, optChangeLink = Some(SummaryChangeLink(
          controllers.userJourney.routes.EmploymentController.subcontractors,
          messages("pages.summary.employsSubcontractors.hiddenChangeText")
        ))
      )
    }.toSeq

    val pensionsSection = employmentView.companyPension.map { paysCompanyPension =>
      SummaryRow(
        id = "paysPension",
        question = messages("pages.summary.paysPension.question"),
        answers = if (paysCompanyPension) {
          Seq(messages("pages.summary.paysPension.answers.true"))
        } else {
          Seq(messages("pages.summary.paysPension.answers.false"))
        }, optChangeLink = Some(SummaryChangeLink(
          controllers.userJourney.routes.EmploymentController.pensions,
          messages("pages.summary.paysPension.hiddenChangeText")
        ))
      )
    }.toSeq

    SummarySection(
      id = "employees",
      sectionHeading = messages("pages.summary.employees.sectionHeading"),
      employingAnyoneSection ++ willBePayingSection ++ cisSection ++ subContractorsSection ++ pensionsSection
    )
  }

  private[services] def buildDirectorsSection(directors: Seq[Director])(implicit messages: Messages) = {
    def directorRow(director: Director, i: Int) = {
      val directorName = director.name.forename match {
        case Some(forename) => s"$forename ${director.name.surname}"
        case None => director.name.surname
      }

      SummaryRow(
        id = "director" + i,
        question = messages("pages.summary.director.question", directorName),
        answers = Seq(director.nino match {
          case Some(nino) => Formatters.ninoFormatter(nino)
          case None => ""
        }),
        optChangeLink = Some(SummaryChangeLink(
          controllers.userJourney.routes.DirectorDetailsController.directorDetails,
          messages("pages.summary.director.hiddenChangeText", directorName)
        ))
      )
    }

    SummarySection(
      id = "directorDetails",
      sectionHeading = messages("pages.summary.directorDetails.sectionHeading"),
      for (d <- directors.zipWithIndex) yield directorRow(d._1, d._2)
    )
  }

  private[services] def buildContactDetails(payeContactDetails: PAYEContact)(implicit messages: Messages) = {
    val payeContactDetailsCall: Call = controllers.userJourney.routes.PAYEContactController.payeContactDetails
    val digitalContact = payeContactDetails.contactDetails.digitalContactDetails
    SummarySection(
      id = "payeContactDetails",
      sectionHeading = messages("pages.summary.payeContactDetails.sectionHeading"),
      Seq(
        SummaryRow(
          id = "contactName",
          question = messages("pages.summary.contactName.question"),
          answers = Seq(payeContactDetails.contactDetails.name),
          optChangeLink = Some(SummaryChangeLink(
            payeContactDetailsCall,
            messages("pages.summary.contactName.hiddenChangeText")
          ))
        ),
        SummaryRow(
          id = "emailPAYEContact",
          question = messages("pages.summary.emailPAYEContact.question"),
          answers = Seq(digitalContact.email match {
            case Some(email) => email
            case _ => messages("pages.summary.emailPAYEContact.answers.noAnswerGiven")
          }),
          optChangeLink = Some(SummaryChangeLink(
            payeContactDetailsCall,
            messages("pages.summary.emailPAYEContact.hiddenChangeText")
          ))
        ),
        SummaryRow(
          id = "mobileNumberPAYEContact",
          question = messages("pages.summary.mobileNumberPAYEContact.question"),
          answers = Seq(digitalContact.mobileNumber match {
            case Some(mobile) => mobile
            case _ => messages("pages.summary.mobileNumberPAYEContact.answers.noAnswerGiven")
          }),
          optChangeLink = Some(SummaryChangeLink(
            payeContactDetailsCall,
            messages("pages.summary.mobileNumberPAYEContact.hiddenChangeText")
          ))
        ),
        SummaryRow(
          id = "phoneNumberPAYEContact",
          question = messages("pages.summary.phoneNumberPAYEContact.question"),
          answers = Seq(digitalContact.phoneNumber match {
            case Some(phone) => phone
            case _ => messages("pages.summary.phoneNumberPAYEContact.answers.noAnswerGiven")
          }),
          optChangeLink = Some(SummaryChangeLink(
            payeContactDetailsCall,
            messages("pages.summary.phoneNumberPAYEContact.hiddenChangeText")
          ))
        ),
        SummaryRow(
          id = "correspondenceAddress",
          question = messages("pages.summary.correspondenceAddress.question"),
          answers = addressToSummaryRowAnswers(payeContactDetails.correspondenceAddress),
          optChangeLink = Some(SummaryChangeLink(
            controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress,
            messages("pages.summary.correspondenceAddress.hiddenChangeText")
          ))
        )
      )
    )
  }
}