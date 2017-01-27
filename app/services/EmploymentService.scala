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

import models.view.{CompanyPension, EmployingStaff, Subcontractors, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import models.api.{Employment => EmploymentAPI}
import utils.DateUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import connectors.{KeystoreConnector, PAYERegistrationConnector, S4LConnector}
import enums.{CacheKeys, DownstreamOutcome}
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

sealed trait SavedResponse
case object S4LSaved extends SavedResponse
case object MongoSaved extends SavedResponse

object EmploymentService extends EmploymentService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val payeRegConnector = PAYERegistrationConnector
  override val s4LService = S4LService
  //$COVERAGE-ON$
}

trait EmploymentService extends CommonService with DateUtil {
  val payeRegConnector: PAYERegistrationConnector
  val s4LService: S4LService
  implicit val formatRecordSet = Json.format[EmploymentView]

  private[services] def viewToAPI(viewData: EmploymentView): Either[EmploymentView, EmploymentAPI] = viewData match {
    case EmploymentView(Some(EmployingStaff(true)), Some(pension), Some(cis), Some(pay)) =>
      Right(EmploymentAPI(true, Some(pension.pensionProvided), cis.hasContractors, toDate(pay.firstPayYear, pay.firstPayMonth, pay.firstPayDay)))
    case EmploymentView(Some(EmployingStaff(false)), _, Some(cis), Some(pay)) =>
      Right(EmploymentAPI(false, None, cis.hasContractors, toDate(pay.firstPayYear, pay.firstPayMonth, pay.firstPayDay)))
    case _ => Left(viewData)
  }

  private[services] def apiToView(apiData: EmploymentAPI): EmploymentView = apiData match {
    case EmploymentAPI(true, Some(pensionProvided), hasContractors, pay) =>
      EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(pensionProvided)), Some(Subcontractors(hasContractors)), Some((FirstPaymentView.apply _).tupled(fromDate(pay.firstPayDate))))
    case EmploymentAPI(false, _, hasContractors, pay) =>
      EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(hasContractors)), Some((FirstPaymentView.apply _).tupled(fromDate(pay.firstPayDate))))
  }

  def fetchEmploymentView()(implicit hc: HeaderCarrier): Future[Option[EmploymentView]] =
    s4LService.fetchAndGet(CacheKeys.Employment.toString) flatMap {
      case Some(employment) => Future.successful(Some(employment))
      case None => for {
        regID <- fetchRegistrationID
        regResponse <- payeRegConnector.getEmployment(regID)
      } yield regResponse match {
        case Some(employment) => Some(apiToView(employment))
        case None => Some(EmploymentView(None, None, None, None))
      }
    }

  def saveEmploymentView(regId: String)(implicit hc: HeaderCarrier): Future[SavedResponse] =
    fetchEmploymentView() flatMap {
      case Some(viewData) => viewToAPI(viewData) match {
        case Left(view) => s4LService.saveForm[EmploymentView](CacheKeys.Employment.toString, view) map(_ => S4LSaved)
        case Right(api) => payeRegConnector.upsertEmployment(regId, api) flatMap(_ => {
          s4LService.clear() map {
            _ => MongoSaved
          }
        })
      }
    }
}
