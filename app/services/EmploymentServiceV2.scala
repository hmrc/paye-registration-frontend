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

import java.time.LocalDate

import com.google.inject.Inject
import connectors.{IncorporationInformationConnector, PAYERegistrationConnector}
import controllers.exceptions.GeneralException
import enums.CacheKeys
import models.api.{Employing, EmploymentV2 => EmploymentAPI}
import models.external.CurrentProfile
import models.view.{EmployingAnyone, WillBePaying, EmployingStaffV2 => EmploymentView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.SystemDate

import scala.concurrent.Future

class EmploymentServiceV2Impl @Inject()(val s4LService: S4LService,
                                        val payeRegConnector: PAYERegistrationConnector,
                                        val iiService: IncorporationInformationService) extends EmploymentServiceV2 {
  override def now: LocalDate = SystemDate.getSystemDate.toLocalDate
}

trait EmploymentServiceV2 {

  def now: LocalDate

  val iiService: IncorporationInformationService
  val s4LService: S4LService
  val payeRegConnector: PAYERegistrationConnector

  private[services] def viewToApi(viewData: EmploymentView): Either[EmploymentView, EmploymentAPI] = viewData match {
    case EmploymentView(Some(EmployingAnyone(true, Some(date))), _, Some(cons), Some(subcontractors), Some(pension)) =>
      Right(EmploymentAPI(Employing.alreadyEmploying, date, cons, if(cons) subcontractors else false, Some(pension)))
    case EmploymentView(employingAnyone@(None | Some(EmployingAnyone(false,_))), Some(willBePaying), Some(true), Some(subcontractors), _) =>
      Right(EmploymentAPI(returnEmployingEnum(employingAnyone,Some(willBePaying)), returnEmployingDate(willBePaying), true, subcontractors, None))
    case EmploymentView(employingAnyone@(None | Some(EmployingAnyone(false,_))), Some(willBePaying), Some(false),_, _) =>
      Right(EmploymentAPI(returnEmployingEnum(employingAnyone,Some(willBePaying)), returnEmployingDate(willBePaying), false, false, None))
    case _ => Left(viewData)
  }

  private[services] def apiToView(employmentAPI: EmploymentAPI, incorpDate: Option[LocalDate]): EmploymentView = employmentAPI match {
    case EmploymentAPI(enumValue, date, cis, subcontractors, pensions) =>
      val (employingAnyone, willBePaying) = enumToTuple(enumValue, date, incorpDate)
      EmploymentView(employingAnyone, willBePaying, Some(cis), if(cis) Some(subcontractors) else None, employingAnyone.filter(_.employing).flatMap(_ => pensions))
  }

  private def returnEmployingDate(willBePaying: WillBePaying):LocalDate = {
    willBePaying.beforeSixApril.fold(now)(b => if(!b) LocalDate.of(now.getYear, 4, 6) else now)
  }

  private def enumToTuple(value: Employing.Value, date: LocalDate, incorpDate: Option[LocalDate]): (Option[EmployingAnyone], Option[WillBePaying]) = value match {
    case Employing.alreadyEmploying   => (Some(EmployingAnyone(true, Some(date))), None)
    case Employing.notEmploying       => employingAnyonConverter((Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None))),incorpDate)
    case Employing.willEmployThisYear => employingAnyonConverter((Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(true)))), incorpDate)
    case Employing.willEmployNextYear => employingAnyonConverter((Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(false)))), incorpDate)
  }

  private def employingAnyonConverter(tuple: (Option[EmployingAnyone], Option[WillBePaying]), incorpDate: Option[LocalDate]) : (Option[EmployingAnyone], Option[WillBePaying]) =
    incorpDate.fold((Option.empty[EmployingAnyone], tuple._2))(_ => tuple)

  private def returnEmployingEnum(employingAnyone: Option[EmployingAnyone], willBePaying: Option[WillBePaying]): Employing.Value = {
    (employingAnyone, willBePaying) match {
      case (Some(EmployingAnyone(true, _)), _)            => Employing.alreadyEmploying
      case (_, Some(WillBePaying(false, _)))              => Employing.notEmploying
      case (_, Some(WillBePaying(true, Some(false))))     => Employing.willEmployNextYear
      case (_, Some(WillBePaying(true, _)))               => Employing.willEmployThisYear

    }
  }

  def fetchEmploymentView(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    s4LService.fetchAndGet[EmploymentView](CacheKeys.EmploymentV2.toString, cp.registrationID) flatMap {
      case Some(employment) => Future.successful(employment)
      case None             => payeRegConnector.getEmploymentV2(cp.registrationID) flatMap { employment =>
        iiService.getIncorporationDate(cp.registrationID, cp.companyTaxRegistration.transactionId) map {
          date =>  employment.fold(EmploymentView(None, None, None, None, None))(e => apiToView(e, date))
        }
      }
    } recover {
      case e: Exception => throw GeneralException(s"[EmploymentServiceV2][fetchEmploymentView] an error occured for regId ${cp.registrationID} with error - ${e.getMessage}")
    }
  }

  private[services] def saveEmploymentView(regId: String, viewData: EmploymentView)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    viewToApi(viewData).fold(
      view => s4LService.saveForm[EmploymentView](CacheKeys.EmploymentV2.toString, view, regId) map(_ => view),
      api  => for {
        _ <- payeRegConnector.upsertEmploymentV2(regId, api)
        _ <- s4LService.clear(regId)
      } yield viewData
    ) recover {
      case e: Exception => throw GeneralException(s"[EmploymentServiceV2][saveEmploymentView] an error occured for regId $regId with error - ${e.getMessage}")
    }
  }

  def fetchAndUpdateViewModel(f: EmploymentView => EmploymentView)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    fetchEmploymentView flatMap(viewModel => saveEmploymentView(cp.registrationID, f(viewModel)))
  }

  def saveEmployingAnyone(employingAnyone: EmployingAnyone)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    fetchAndUpdateViewModel(_.copy(employingAnyone = Some(employingAnyone)))
  }

  def saveWillEmployAnyone(willBePaying: WillBePaying)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    fetchAndUpdateViewModel(_.copy(willBePaying = Some(willBePaying)))
  }

  def saveConstructionIndustry(construction: Boolean)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    fetchAndUpdateViewModel(_.copy(construction = Some(construction)))
  }

  def saveSubcontractors(subcontractors: Boolean)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    fetchAndUpdateViewModel(_.copy(subcontractors = Some(subcontractors)))
  }

  def savePensionPayment(companyPension: Boolean)(implicit hc: HeaderCarrier, cp: CurrentProfile): Future[EmploymentView] = {
    fetchAndUpdateViewModel(_.copy(companyPension = Some(companyPension)))
  }
}