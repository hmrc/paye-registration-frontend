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

import java.time.LocalDate

import com.google.inject.Inject
import connectors.PAYERegistrationConnector
import controllers.exceptions.GeneralException
import enums.CacheKeys
import forms.employmentDetails.EmployingStaffForm
import models.api.{Employing, Employment}
import models.external.CurrentProfile
import models.view.{EmployingAnyone, EmployingStaff, WillBePaying}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.SystemDate

import scala.concurrent.Future

class EmploymentServiceImpl @Inject()(val s4LService: S4LService,
                                      val payeRegConnector: PAYERegistrationConnector,
                                      val iiService: IncorporationInformationService) extends EmploymentService {
  override def now: LocalDate = SystemDate.getSystemDate.toLocalDate
}

trait EmploymentService {

  def now: LocalDate

  val iiService: IncorporationInformationService
  val s4LService: S4LService
  val payeRegConnector: PAYERegistrationConnector

  private[services] def viewToApi(viewData: EmployingStaff): Either[EmployingStaff, Employment] = viewData match {
    case EmployingStaff(Some(EmployingAnyone(true, Some(date))), _, Some(true), Some(subcontractors), Some(pension)) =>
      Right(Employment(Employing.alreadyEmploying, date, true, subcontractors, Some(pension)))
    case EmployingStaff(Some(EmployingAnyone(true, Some(date))), _, Some(false), _, Some(pension)) =>
      Right(Employment(Employing.alreadyEmploying, date, false, false, Some(pension)))
    case EmployingStaff(employingAnyone@(None | Some(EmployingAnyone(false, _))), Some(willBePaying), Some(true), Some(subcontractors), _) =>
      Right(Employment(returnEmployingEnum(employingAnyone, Some(willBePaying)), returnEmployingDate(willBePaying), true, subcontractors, None))
    case EmployingStaff(employingAnyone@(None | Some(EmployingAnyone(false, _))), Some(willBePaying), Some(false), _, _) =>
      Right(Employment(returnEmployingEnum(employingAnyone, Some(willBePaying)), returnEmployingDate(willBePaying), false, false, None))
    case _ => Left(viewData)
  }

  private[services] def apiToView(employmentAPI: Employment, incorpDate: Option[LocalDate]): EmployingStaff = employmentAPI match {
    case Employment(enumValue, date, cis, subcontractors, pensions) =>
      val (employingAnyone, willBePaying) = enumToTuple(enumValue, date, incorpDate)
      EmployingStaff(employingAnyone, willBePaying, Some(cis), if (cis) Some(subcontractors) else None, employingAnyone.filter(_.employing).flatMap(_ => pensions))
  }

  private def returnEmployingDate(willBePaying: WillBePaying): LocalDate = {
    willBePaying.beforeSixApril.fold(now)(b => if (!b) LocalDate.of(now.getYear, 4, 6) else now)
  }

  private def enumToTuple(value: Employing.Value, date: LocalDate, incorpDate: Option[LocalDate]): (Option[EmployingAnyone], Option[WillBePaying]) = value match {
    case Employing.alreadyEmploying => (Some(EmployingAnyone(true, Some(date))), None)
    case Employing.notEmploying => employingAnyoneConverter((Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None))), incorpDate)
    case Employing.willEmployThisYear => employingAnyoneConverter((Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(true)))), incorpDate)
    case Employing.willEmployNextYear => employingAnyoneConverter((Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(false)))), incorpDate)
  }

  private def employingAnyoneConverter(tuple: (Option[EmployingAnyone], Option[WillBePaying]), incorpDate: Option[LocalDate]): (Option[EmployingAnyone], Option[WillBePaying]) = {
    val (paidEmployees, willBePaying) = tuple
    val updatedWillBePayingBlock = if (EmployingStaffForm.isRequiredBeforeNewTaxYear(now)) willBePaying else willBePaying.map(_.copy(beforeSixApril = None))

    incorpDate.fold((Option.empty[EmployingAnyone], updatedWillBePayingBlock))(_ => (paidEmployees, updatedWillBePayingBlock))
  }

  private def returnEmployingEnum(employingAnyone: Option[EmployingAnyone], willBePaying: Option[WillBePaying]): Employing.Value = {
    (employingAnyone, willBePaying) match {
      case (Some(EmployingAnyone(true, _)), _) => Employing.alreadyEmploying
      case (_, Some(WillBePaying(false, _))) => Employing.notEmploying
      case (_, Some(WillBePaying(true, Some(false)))) => Employing.willEmployNextYear
      case (_, Some(WillBePaying(true, _))) => Employing.willEmployThisYear
    }
  }

  def fetchEmployingStaff(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    s4LService.fetchAndGet[EmployingStaff](CacheKeys.EmploymentV2.toString, cp.registrationID) flatMap {
      case Some(employment) => Future.successful(employment)
      case None => payeRegConnector.getEmployment(cp.registrationID) flatMap { employment =>
        iiService.getIncorporationDate(cp.registrationID, cp.companyTaxRegistration.transactionId) map {
          date => employment.fold(EmployingStaff(None, None, None, None, None))(e => apiToView(e, date))
        }
      }
    } recover {
      case e: Exception => throw GeneralException(s"[EmploymentService][fetchEmployingStaff] an error occured for regId ${cp.registrationID} with error - ${e.getMessage}")
    }
  }

  private[services] def saveEmployingStaff(regId: String, viewData: EmployingStaff)(implicit hc: HeaderCarrier): Future[EmployingStaff] = {
    viewToApi(viewData).fold(
      view => s4LService.saveForm[EmployingStaff](CacheKeys.EmploymentV2.toString, view, regId) map (_ => view),
      api => for {
        _ <- payeRegConnector.upsertEmployment(regId, api)
        _ <- s4LService.clear(regId)
      } yield viewData
    ) recover {
      case e: Exception => throw GeneralException(s"[EmploymentService][saveEmployingStaff] an error occured for regId $regId with error - ${e.getMessage}")
    }
  }

  def fetchAndUpdateViewModel(f: EmployingStaff => EmployingStaff)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    fetchEmployingStaff flatMap (viewModel => saveEmployingStaff(cp.registrationID, f(viewModel)))
  }

  def saveEmployingAnyone(employingAnyone: EmployingAnyone)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    fetchAndUpdateViewModel(_.copy(employingAnyone = Some(employingAnyone)))
  }

  def saveWillEmployAnyone(willBePaying: WillBePaying)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    fetchAndUpdateViewModel(_.copy(willBePaying = Some(willBePaying)))
  }

  def saveConstructionIndustry(construction: Boolean)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    fetchAndUpdateViewModel(_.copy(construction = Some(construction)))
  }

  def saveSubcontractors(subcontractors: Boolean)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    fetchAndUpdateViewModel(_.copy(subcontractors = Some(subcontractors)))
  }

  def savePensionPayment(companyPension: Boolean)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmployingStaff] = {
    fetchAndUpdateViewModel(_.copy(companyPension = Some(companyPension)))
  }
}