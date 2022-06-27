/*
 * Copyright 2022 HM Revenue & Customs
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
import models.SummaryListRowUtils.{optSummaryListRowBoolean, optSummaryListRowSeq, optSummaryListRowString}
import models.api.{CompanyDetails, Director, Employment, PAYEContact, SICCode, PAYERegistration => PAYERegistrationAPI}
import models.{Address, DigitalContactDetails}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Formatters

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class SummaryService @Inject()(val payeRegistrationConnector: PAYERegistrationConnector,
                               val employmentService: EmploymentService,
                               val iiService: IncorporationInformationService,
                               val messagesApi: MessagesApi
                              )(implicit ec: ExecutionContext) {

  def getEmploymentSectionSummary(regId: String, txId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[SummaryList] = {
    implicit val messages: Messages = messagesApi.preferred(request)

    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
      incorporationDate <- iiService.getIncorporationDate(regId, txId)
    } yield employmentSectionSummary(regResponse, incorporationDate)
  }

  def getCompletionCapacitySummary(regId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[SummaryList] = {
    implicit val messages: Messages = messagesApi.preferred(request)
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield completionCapacitySummary(regResponse)
  }

  def getCompanyDetailsSummary(regId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[SummaryList] = {
    implicit val messages: Messages = messagesApi.preferred(request)
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield companyDetailsSummary(regResponse)
  }

  def getBusinessContactSummary(regId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[SummaryList] = {
    implicit val messages: Messages = messagesApi.preferred(request)
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield businessContactSummary(regResponse)
  }

  def getDirectorsSummary(regId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[SummaryList] = {
    implicit val messages: Messages = messagesApi.preferred(request)
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield directorsSummary(regResponse)
  }

  def getContactDetailsSummary(regId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[SummaryList] = {
    implicit val messages: Messages = messagesApi.preferred(request)
    for {
      regResponse <- payeRegistrationConnector.getRegistration(regId)
    } yield contactDetailsSummary(regResponse)
  }

  private[services] def employmentSectionSummary(apiModel: PAYERegistrationAPI, incorporationDate: Option[LocalDate])(implicit messages: Messages): SummaryList =
    SummaryList(buildEmploymentSectionFromView(apiModel.employmentInfo, incorporationDate))

  private[services] def completionCapacitySummary(apiModel: PAYERegistrationAPI)(implicit messages: Messages): SummaryList =
    SummaryList(buildCompletionCapacitySection(apiModel.completionCapacity))

  private[services] def companyDetailsSummary(apiModel: PAYERegistrationAPI)(implicit messages: Messages): SummaryList =
    SummaryList(buildCompanyDetailsSection(apiModel.companyDetails, apiModel.sicCodes))

  private[services] def businessContactSummary(apiModel: PAYERegistrationAPI)(implicit messages: Messages): SummaryList =
    SummaryList(buildBusinessContactDetailsSection(apiModel.companyDetails.businessContactDetails))

  private[services] def directorsSummary(apiModel: PAYERegistrationAPI)(implicit messages: Messages): SummaryList =
    SummaryList(buildDirectorsSection(apiModel.directors))

  private[services] def contactDetailsSummary(apiModel: PAYERegistrationAPI)(implicit messages: Messages): SummaryList =
    SummaryList(buildContactDetails(apiModel.payeContact))

  private[services] def buildCompletionCapacitySection(capacity: String)(implicit messages: Messages): Seq[SummaryListRow] = {
    val displayCapacity: String = Try {
      UserCapacity.fromString(capacity)
    } match {
      case Success(UserCapacity.director) => messages("pages.summary.completionCapacity.answers.director")
      case Success(UserCapacity.agent) => messages("pages.summary.completionCapacity.answers.agent")
      case Success(UserCapacity.secretary) => messages("pages.summary.completionCapacity.answers.companysecretary")
      case _ => capacity
    }

    val completionCapacity = optSummaryListRowSeq(
      messages("pages.summary.completionCapacity.question"),
      Some(Seq(displayCapacity)),
      Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
    )

    Seq(
      completionCapacity
    ).flatten
  }

  private[services] def buildCompanyDetailsSection(companyDetails: CompanyDetails, sicCodes: List[SICCode])(implicit messages: Messages): Seq[SummaryListRow] = {

    val tradingName = optSummaryListRowSeq(
      messages("pages.tradingName.description"),
      Some(List(companyDetails.tradingName match {
        case Some(tName) => tName
        case _ => messages("pages.summary.tradingName.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.CompanyDetailsController.tradingName.url)
    )

    val roAddress = optSummaryListRowSeq(
      messages("pages.summary.roAddress.question"),
      Some(addressToSummaryRowAnswers(companyDetails.roAddress)),
      None
    )

    val ppobAddress = optSummaryListRowSeq(
      messages("pages.summary.ppobAddress.question"),
      Some(addressToSummaryRowAnswers(companyDetails.ppobAddress)),
      Some(controllers.userJourney.routes.CompanyDetailsController.ppobAddress.url)
    )

    val natureOfBusiness = optSummaryListRowSeq(
      messages("pages.summary.natureOfBusiness.question"),
      Some(List(sicCodes.head.description.getOrElse {
        throw new APIConversionException("No nature of business provided for summary")
      })),
      Some(controllers.userJourney.routes.NatureOfBusinessController.natureOfBusiness.url)
    )

    Seq(
      tradingName,
      roAddress,
      ppobAddress,
      natureOfBusiness
    ).flatten
  }

  private[services] def buildBusinessContactDetailsSection(businessContactDetails: DigitalContactDetails)(implicit messages: Messages): Seq[SummaryListRow] = {

    val businessEmail = optSummaryListRowSeq(
      messages("pages.summary.businessEmail.question"),
      Some(List(businessContactDetails.email match {
        case Some(email) => email
        case _ => messages("pages.summary.businessEmail.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
    )

    val mobileNumber = optSummaryListRowSeq(
      messages("pages.summary.mobileNumber.question"),
      Some(List(businessContactDetails.mobileNumber match {
        case Some(mobile) => mobile
        case _ => messages("pages.summary.mobileNumber.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
    )

    val businessTelephone = optSummaryListRowSeq(
      messages("pages.summary.businessTelephone.question"),
      Some(List(businessContactDetails.phoneNumber match {
        case Some(businessPhoneNumber) => businessPhoneNumber
        case _ => messages("pages.summary.businessTelephone.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails.url)
    )

    Seq(
      businessEmail,
      mobileNumber,
      businessTelephone
    ).flatten
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

  private[services] def buildEmploymentSectionFromView(oEmployment: Employment, incorpDate: Option[LocalDate])(implicit messages: Messages): Seq[SummaryListRow] = {
    val employmentView = employmentService.apiToView(oEmployment, incorpDate)

    val employingAnyoneSection = optSummaryListRowBoolean(
      messages("pages.summary.employing.question"),
      employmentView.employingAnyone.map(_.employing),
      Some(controllers.userJourney.routes.EmploymentController.paidEmployees.url)
    )

    val willBePayingSection = optSummaryListRowBoolean(
      messages("pages.summary.willBePaying.question"),
      employmentView.willBePaying.map(_.willPay),
      Some(controllers.userJourney.routes.EmploymentController.employingStaff.url)
    )


    val cisSection = optSummaryListRowBoolean(
      messages("pages.summary.inConstructionIndustry.question"),
      Some(employmentView.construction).flatten,
      Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)
    )

    val subContractorsSection = optSummaryListRowBoolean(
      messages("pages.summary.employsSubcontractors.question"),
      Some(employmentView.subcontractors).flatten,
      Some(controllers.userJourney.routes.EmploymentController.subcontractors.url)
    )

    val pensionsSection =
      optSummaryListRowBoolean(
        messages("pages.summary.paysPension.question"),
        Some(employmentView.companyPension).flatten,
        Some(controllers.userJourney.routes.EmploymentController.pensions.url)
      )


    Seq(
      employingAnyoneSection,
      willBePayingSection,
      cisSection,
      subContractorsSection,
      pensionsSection
    ).flatten

  }

  private[services] def buildDirectorsSection(directors: Seq[Director])(implicit messages: Messages): Seq[SummaryListRow] = {

    def directorRowName(director: Director, i: Int) = {
      val directorName = director.nino match {
        case _ => s"${director.name.forename.get} ${director.name.surname}"
      }
      directorName
    }

    def directorRowNino(director: Director, i: Int) = {
      val directorNino = director.nino match {
        case Some(nino) => Formatters.ninoFormatter(nino)
        case None => ""
      }

      directorNino
    }

    val completionCapacity = for (d <- directors.zipWithIndex) yield {
        optSummaryListRowString(
          messages("pages.summary.director.question", directorRowName(d._1, d._2)),
          Some(directorRowNino(d._1, d._2)),
          Some(controllers.userJourney.routes.DirectorDetailsController.directorDetails.url)
        ).get
    }

    Seq(
      completionCapacity
    ).flatten
  }

  private[services] def buildContactDetails(payeContactDetails: PAYEContact)(implicit messages: Messages): Seq[SummaryListRow] = {
    val digitalContact = payeContactDetails.contactDetails.digitalContactDetails

    val contactName = optSummaryListRowSeq(
      messages("pages.summary.contactName.question"),
      Some(Seq(payeContactDetails.contactDetails.name)),
      Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
    )

    val emailContact = optSummaryListRowSeq(
      messages("pages.summary.emailPAYEContact.question"),
      Some(Seq(digitalContact.email match {
        case Some(email) => email
        case _ => messages("pages.summary.emailPAYEContact.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
    )

    val mobileContact = optSummaryListRowSeq(
      messages("pages.summary.mobileNumberPAYEContact.question"),
      Some(Seq(digitalContact.mobileNumber match {
        case Some(mobile) => mobile
        case _ => messages("pages.summary.mobileNumberPAYEContact.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
    )

    val phoneNumber = optSummaryListRowSeq(
      messages("pages.summary.phoneNumberPAYEContact.question"),
      Some(Seq(digitalContact.phoneNumber match {
        case Some(phone) => phone
        case _ => messages("pages.summary.phoneNumberPAYEContact.answers.noAnswerGiven")
      })),
      Some(controllers.userJourney.routes.PAYEContactController.payeContactDetails.url)
    )

    val correspondanceAddress = optSummaryListRowSeq(
      messages("pages.summary.correspondenceAddress.question"),
      Some(addressToSummaryRowAnswers(payeContactDetails.correspondenceAddress)),
      Some(controllers.userJourney.routes.PAYEContactController.payeCorrespondenceAddress.url)
    )

    Seq(
      contactName,
      emailContact,
      mobileContact,
      phoneNumber,
      correspondanceAddress
    ).flatten
  }
}