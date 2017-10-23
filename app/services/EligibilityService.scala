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
import enums.{CacheKeys, DownstreamOutcome}
import models.api.{Eligibility => EligibilityAPI}
import models.view.{CompanyEligibility, DirectorEligibility, Eligibility => EligibilityView}

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class EligibilityService @Inject()(val payeRegConnector: PAYERegistrationConnector, val s4lService: S4LService) extends EligibilitySrv

trait EligibilitySrv {
  val payeRegConnector : PAYERegistrationConnect
  val s4lService : S4LSrv

  private[services] def apiToView(data: EligibilityAPI) : EligibilityView = {
    EligibilityView(Some(CompanyEligibility(data.companyEligibility)), Some(DirectorEligibility(data.directorEligibility)))
  }

  private[services] def isCompleteData(data: EligibilityView) : Either[EligibilityView, EligibilityAPI] = data match {
    case EligibilityView(Some(e1), Some(e2)) => Right(EligibilityAPI(e1.ineligible, e2.eligible))
    case _                                   => Left(data)
  }

  private[services] def convertOrCreateEligibilityView(oAPI: Option[EligibilityAPI]): EligibilityView = {
    oAPI match {
      case Some(data) => apiToView(data)
      case None       => EligibilityView(None, None)
    }
  }

  def getEligibility(regId: String)(implicit hc: HeaderCarrier) : Future[EligibilityView]= {
    s4lService.fetchAndGet[EligibilityView](CacheKeys.Eligibility.toString, regId) flatMap {
      case Some(e) => Future.successful(e)
      case None    => for {
        oDetails <- payeRegConnector.getEligibility(regId)
      } yield convertOrCreateEligibilityView(oDetails)
    }
  }

  def submitEligibility(regId: String, data: EligibilityView)(implicit hc: HeaderCarrier) : Future[DownstreamOutcome.Value] = {
    isCompleteData(data).fold(
      incomplete =>
        s4lService.saveForm[EligibilityView](CacheKeys.Eligibility.toString, data, regId).map(
          _ => DownstreamOutcome.Success
        ),
      complete =>
        for {
          _ <- payeRegConnector.upsertEligibility(regId, complete)
          _ <- s4lService.clear(regId)
        } yield DownstreamOutcome.Success
    )
  }

  def saveCompanyEligibility(regId: String, viewData: CompanyEligibility)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getEligibility(regId) flatMap {
      storedData => submitEligibility(regId, EligibilityView(Some(CompanyEligibility(viewData.ineligible)), storedData.directorEligible))
    }
  }

  def saveDirectorEligibility(regId: String, viewData: DirectorEligibility)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getEligibility(regId) flatMap {
      storedData => submitEligibility(regId, EligibilityView(storedData.companyEligible, Some(DirectorEligibility(viewData.eligible))))
    }
  }

}
