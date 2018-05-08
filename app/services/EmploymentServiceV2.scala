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
import connectors.PAYERegistrationConnector
import controllers.exceptions.GeneralException
import enums.CacheKeys
import models.view.{EmployingAnyone, WillBePaying, EmployingStaffV2 => EmploymentView}
import models.api.{Employing, EmploymentV2 => EmploymentAPI}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.SystemDate

import scala.concurrent.Future

class EmploymentServiceV2Impl @Inject()(val s4LService: S4LService,
                                        val payeRegConnector: PAYERegistrationConnector) extends EmploymentServiceV2 {
  override def now: LocalDate = SystemDate.getSystemDate.toLocalDate
}

trait EmploymentServiceV2 {

  def now: LocalDate

  val s4LService: S4LService
  val payeRegConnector: PAYERegistrationConnector

//  private[services] def viewToApi(viewData: EmploymentView): Either[EmploymentView, EmploymentAPI] = viewData match {
//    case EmploymentView(Some(EmployingAnyone(true, Some(date))), _, Some(cis), Some(subcontractors), Some(pension)) =>
//      Right(EmploymentAPI(Employing.alreadyEmploying, date, cis, subcontractors, Some(pension)))
//    case EmploymentView(Some(employingAnyong), Some(willBePaying), Some(true), Some(subcontractors), _) =>
//      Right(EmploymentAPI(returnEmployingEnum(EmployingAnyone(false, None), willBePaying), returnEmployingDate(willBePaying), true, subcontractors, None))
//    case EmploymentView(Some(EmployingAnyone(false, None)), Some(willBePaying), Some(false), _, _) =>
//      Right(EmploymentAPI(returnEmployingEnum(EmployingAnyone(false, None), willBePaying), returnEmployingDate(willBePaying), false, false, None))
//    case _ => Left(viewData)
//  }

  private[services] def viewToApi(viewData: EmploymentView): Either[EmploymentView, EmploymentAPI] = viewData match {
    case EmploymentView(Some(EmployingAnyone(true, Some(date))), _, Some(true), Some(subcontractors), Some(pension)) =>
      Right(EmploymentAPI(Employing.alreadyEmploying, date, true, subcontractors, Some(pension)))
      case EmploymentView(Some(EmployingAnyone(true, Some(date))), _, Some(false),_, Some(pension)) =>
  Right(EmploymentAPI(Employing.alreadyEmploying, date, false, false, Some(pension)))
    case EmploymentView(None, Some(willBePaying), Some(true), Some(subcontractors), _) =>
      Right(EmploymentAPI(returnEmployingEnum(None,Some(willBePaying)), returnEmployingDate(willBePaying), true, subcontractors,None))
    case EmploymentView(None, Some(willBePaying), Some(false),_, _) =>
      Right(EmploymentAPI(returnEmployingEnum(None,Some(willBePaying)), returnEmployingDate(willBePaying), false, false, None))
    case _ => Left(viewData)
  }

  private[services] def apiToView(employmentAPI: EmploymentAPI): EmploymentView = employmentAPI match {
    case EmploymentAPI(enumValue, date, cis, subcontractors, pensions) =>
      val (employingAnyone, willBePaying) = enumToTuple(enumValue, date)
      EmploymentView(employingAnyone, willBePaying, Some(cis), if(cis) Some(subcontractors) else None, employingAnyone.filter(_.employing).flatMap(_ => pensions))
  }

  private def returnEmployingDate(willBePaying: WillBePaying):LocalDate = {
    willBePaying.beforeSixApril.fold(now)(b => if(!b) LocalDate.of(now.getYear, 4, 6) else now)
  }

  private def enumToTuple(value: Employing.Value, date: LocalDate): (Option[EmployingAnyone], Option[WillBePaying]) = value match {
    case Employing.alreadyEmploying   => (Some(EmployingAnyone(true, Some(date))), None)
    case Employing.notEmploying       => (Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None)))
    case Employing.willEmployThisYear => (Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(true))))
    case Employing.willEmployNextYear => (Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(false))))
  }

//  private def returnEmployingEnum(employingAnyone: EmployingAnyone, willBePaying: WillBePaying): Employing.Value = {
//    (employingAnyone, willBePaying) match {
//      case (EmployingAnyone(false,_), WillBePaying(false,_))          => Employing.notEmploying
//      case (EmployingAnyone(false,_), WillBePaying(true,Some(false))) => Employing.willEmployNextYear
//      case (EmployingAnyone(false,_), WillBePaying(true,_))           => Employing.willEmployThisYear
//    }
//  }

  private def returnEmployingEnum(employingAnyone: Option[EmployingAnyone], willBePaying: Option[WillBePaying]): Employing.Value = {
    (employingAnyone, willBePaying) match {
      case (Some(EmployingAnyone(true, _)), _)            => Employing.alreadyEmploying
      case (_, Some(WillBePaying(false, _)))              => Employing.notEmploying
      case (_, Some(WillBePaying(true, Some(false))))     => Employing.willEmployNextYear
      case (_, Some(WillBePaying(true, _)))               => Employing.willEmployThisYear

    }
  }

  def fetchEmploymentView(regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    s4LService.fetchAndGet[EmploymentView](CacheKeys.EmploymentV2.toString, regId) flatMap {
      case Some(employment) => Future.successful(employment)
      case None             => payeRegConnector.getEmploymentV2(regId) map {
        _.fold(EmploymentView(None, None, None, None, None))(apiToView)
      }
    } recover {
      case e: Exception => throw GeneralException(s"[EmploymentServiceV2][fetchEmploymentView] an error occured for regId $regId with error - ${e.getMessage}")
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

  def fetchAndUpdateViewModel(regId: String)(f: EmploymentView => EmploymentView)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    fetchEmploymentView(regId) flatMap(viewModel => saveEmploymentView(regId, f(viewModel)))
  }

  def saveEmployingAnyone(regId: String, employingAnyone: EmployingAnyone)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    fetchAndUpdateViewModel(regId)(_.copy(employingAnyone = Some(employingAnyone)))
  }

  def saveWillEmployAnyone(regId: String, willBePaying: WillBePaying)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    fetchAndUpdateViewModel(regId)(_.copy(willBePaying = Some(willBePaying)))
  }

  def saveConstructionIndustry(regId: String, construction: Boolean)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    fetchAndUpdateViewModel(regId)(_.copy(construction = Some(construction)))
  }

  def saveSubcontractors(regId: String, subcontractors: Boolean)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    fetchAndUpdateViewModel(regId)(_.copy(subcontractors = Some(subcontractors)))
  }

  def savePensionPayment(regId: String, companyPension: Boolean)(implicit hc: HeaderCarrier): Future[EmploymentView] = {
    fetchAndUpdateViewModel(regId)(_.copy(companyPension = Some(companyPension)))
  }
}
