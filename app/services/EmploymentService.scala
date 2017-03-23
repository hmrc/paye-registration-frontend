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

import javax.inject.{Inject, Singleton}

import connectors.{PAYERegistrationConnect, PAYERegistrationConnector}
import enums.CacheKeys
import models.api.{Employment => EmploymentAPI}
import models.view.{CompanyPension, EmployingStaff, Subcontractors, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.DateUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait SavedResponse
case object S4LSaved extends SavedResponse
case class MongoSaved(employment: EmploymentView) extends SavedResponse

@Singleton
class EmploymentService @Inject()(payeRegistrationConn: PAYERegistrationConnector, s4LServ: S4LService) extends EmploymentSrv {
  override val payeRegConnector = payeRegistrationConn
  override val s4LService = s4LServ
}

trait EmploymentSrv extends DateUtil {

  val payeRegConnector: PAYERegistrationConnect
  val s4LService: S4LSrv

  implicit val formatRecordSet = Json.format[EmploymentView]

  private[services] def viewToAPI(viewData: EmploymentView): Either[EmploymentView, EmploymentAPI] = viewData match {
    case EmploymentView(Some(EmployingStaff(true)), Some(pension), Some(cis), Some(pay)) =>
      Right(EmploymentAPI(true, Some(pension.pensionProvided), cis.hasContractors, pay.firstPayDate))
    case EmploymentView(Some(EmployingStaff(false)), _, Some(cis), Some(pay)) =>
      Right(EmploymentAPI(false, None, cis.hasContractors, pay.firstPayDate))
    case _ => Left(viewData)
  }

  private[services] def apiToView(apiData: EmploymentAPI): EmploymentView = apiData match {
    case EmploymentAPI(true, Some(pensionProvided), hasContractors, pay) =>
      EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(pensionProvided)), Some(Subcontractors(hasContractors)), Some(FirstPaymentView(apiData.firstPayDate)))
    case EmploymentAPI(false, _, hasContractors, pay) =>
      EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(hasContractors)), Some(FirstPaymentView(apiData.firstPayDate)))
  }

  def fetchEmploymentView(regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] =
    s4LService.fetchAndGet(CacheKeys.Employment.toString, regId) flatMap {
      case Some(employment) => Future.successful(employment)
      case None => for {
        regResponse <- payeRegConnector.getEmployment(regId)
      } yield regResponse match {
        case Some(employment) => apiToView(employment)
        case None => EmploymentView(None, None, None, None)
      }
    }

  def saveEmploymentView(viewData: EmploymentView, regId: String)(implicit hc: HeaderCarrier): Future[SavedResponse] =
    viewToAPI(viewData) match {
      case Left(view) => s4LService.saveForm[EmploymentView](CacheKeys.Employment.toString, view, regId) map(_ => S4LSaved)
      case Right(api) => payeRegConnector.upsertEmployment(regId, api) map { _ => MongoSaved(viewData) }
    }

  def saveEmployment(viewData: EmploymentView, regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] =
    saveEmploymentView(viewData, regId) flatMap {
      case MongoSaved(_) => s4LService.clear(regId) map (_ => viewData)
      case _ => Future.successful(viewData)
    }

  def saveEmployingStaff(viewData: EmployingStaff, regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] =
    fetchEmploymentView(regId) flatMap {
      employment => {
        saveEmployment(EmploymentView(Some(viewData), employment.companyPension, employment.subcontractors, employment.firstPayment), regId)
      }
    }

  def saveCompanyPension(viewData: CompanyPension, regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] =
    fetchEmploymentView(regId) flatMap {
      employment => {
        saveEmployment(EmploymentView(employment.employing, Some(viewData), employment.subcontractors, employment.firstPayment), regId)
      }
    }

  def saveSubcontractors(viewData: Subcontractors, regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] =
    fetchEmploymentView(regId) flatMap {
      employment => {
        saveEmployment(EmploymentView(employment.employing, employment.companyPension, Some(viewData), employment.firstPayment), regId)
      }
    }

  def saveFirstPayment(viewData: FirstPaymentView, regId: String)(implicit hc: HeaderCarrier): Future[EmploymentView] =
    fetchEmploymentView(regId) flatMap {
      employment => {
        saveEmployment(EmploymentView(employment.employing, employment.companyPension, employment.subcontractors, Some(viewData)), regId)
      }
    }
}
