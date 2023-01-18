/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors._
import enums.{DownstreamOutcome, UserCapacity}
import models.view.CompletionCapacity
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompletionCapacityServiceImpl @Inject()(val payeRegConnector: PAYERegistrationConnector,
                                              val businessRegistrationConnector: BusinessRegistrationConnector)(implicit val ec: ExecutionContext) extends CompletionCapacityService

trait CompletionCapacityService extends Logging {

  val payeRegConnector: PAYERegistrationConnector
  val businessRegistrationConnector: BusinessRegistrationConnector
  implicit val ec: ExecutionContext

  def saveCompletionCapacity(regId: String, completionCapacity: CompletionCapacity)(implicit hc: HeaderCarrier, request: Request[_]): Future[DownstreamOutcome.Value] = {
    payeRegConnector.upsertCompletionCapacity(regId, viewToAPI(completionCapacity)) map {
      _ => DownstreamOutcome.Success
    }
  }

  def getCompletionCapacity(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[CompletionCapacity]] = {
    payeRegConnector.getCompletionCapacity(regId) flatMap {
      case Some(prCC) => Future.successful(Some(apiToView(prCC)))
      case None => businessRegistrationConnector.retrieveCompletionCapacity flatMap {
        case Some(brCC) =>
          payeRegConnector.upsertCompletionCapacity(regId, brCC) flatMap {
            _ => Future.successful(Some(apiToView(brCC)))
          }
        case None =>
          infoLog(s"[getCompletionCapacity] BR document was found for regId $regId but it contained no completion capacity")
          Future.successful(None)
      } recover {
        case e: Throwable =>
          warnLog(s"[getCompletionCapacity] No document was found in business registration for regId $regId: reason ${e.getMessage}")
          None
      }
    }
  }

  private[services] def viewToAPI(completionCapacity: CompletionCapacity): String = {
    completionCapacity.completionCapacity match {
      case UserCapacity.director => UserCapacity.director.toString
      case UserCapacity.agent => UserCapacity.agent.toString
      case UserCapacity.secretary => UserCapacity.secretary.toString
      case UserCapacity.other => completionCapacity.completionCapacityOther
    }
  }

  private[services] def apiToView(completionCapacity: String): CompletionCapacity = {
    completionCapacity.toLowerCase match {
      case "director" => CompletionCapacity(UserCapacity.director, "")
      case "agent" => CompletionCapacity(UserCapacity.agent, "")
      case "company secretary" => CompletionCapacity(UserCapacity.secretary, "")
      case _ => CompletionCapacity(UserCapacity.other, completionCapacity)
    }
  }
}
