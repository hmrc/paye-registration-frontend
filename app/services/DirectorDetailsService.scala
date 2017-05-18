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
import models.api.Director
import models.view.{Directors, Ninos, UserEnteredNino}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RegistrationWhitelist

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DirectorDetailsService @Inject()(
                                        payeRegistrationConn: PAYERegistrationConnector,
                                        coHoAPIServ: IncorporationInformationService,
                                        s4LServ: S4LService) extends DirectorDetailsSrv {

  override val payeRegConnector = payeRegistrationConn
  override val incorpInfoService = coHoAPIServ
  override val s4LService = s4LServ
}

trait DirectorDetailsSrv extends RegistrationWhitelist {
  val payeRegConnector: PAYERegistrationConnect
  val s4LService: S4LSrv
  val incorpInfoService: IncorporationInformationSrv

  implicit val formatRecordSet = Directors.directorMappingFormat

  private[services] def ninosToDirectorsMap(details: Directors, ninos: Ninos)(implicit hc: HeaderCarrier): Map[String, Director] = {
    details.directorMapping.map {
      case (k, v) => k -> v.copy(nino = ninos.ninoMapping.filter(_.id == k).map(_.nino).head)
    }
  }

  private[services] def apiToView(apiData: Seq[Director]): Directors =
    Directors(directorMapping = (apiData.indices.map(_.toString) zip apiData).toMap)

  private[services] def viewToAPI(viewData: Directors): Either[Directors, Seq[Director]] = viewData match {
    case Directors(map) if map.nonEmpty => Right(map.values.toList)
    case _ => Left(viewData)
  }

  private[services] def convertOrRetrieveDirectors(directorList: Seq[Director], transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    directorList match {
      case Nil => for {
        directors <- incorpInfoService.getDirectorDetails(transactionId)
      } yield directors
      case dirList => Future.successful(apiToView(dirList))
    }
  }

  private def saveToS4L(viewData: Directors, regId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    s4LService.saveForm[Directors](CacheKeys.DirectorDetails.toString, viewData, regId).map(_ => viewData)
  }

  def getDirectorDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    s4LService.fetchAndGet(CacheKeys.DirectorDetails.toString, regId) flatMap {
      case Some(directors) => Future.successful(directors)
      case None => for {
        regResponse <- ifRegIdNotWhitelisted(regId){
          payeRegConnector.getDirectors(regId)
        }
        directors <- convertOrRetrieveDirectors(regResponse, transactionId)
        data <- saveToS4L(directors, regId)
      } yield data
    }
  }

  private[services] def saveDirectorDetails(viewModel: Directors, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel) fold(
      incompleteView =>
        saveToS4L(incompleteView, regId) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          details   <- payeRegConnector.upsertDirectors(regId, completeAPI)
          clearData <- s4LService.clear(regId)
        } yield DownstreamOutcome.Success
    )
  }

  def createDisplayNamesMap(directors: Directors): Map[String, String] = {
    directors.directorMapping.map {
      case(k, v) => (k, List(v.name.forename, Some(v.name.surname)).flatten.mkString(" "))
    }
  }

  def createDirectorNinos(directors: Directors): Ninos = {
    Ninos((0 until directors.directorMapping.size).map {
      index => UserEnteredNino(
        index.toString,
        directors.directorMapping.get(index.toString).flatMap(_.nino)
      )
    }.toList)
  }

  def submitNinos(ninos: Ninos, regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getDirectorDetails(regId, transactionId)
      outcome <- saveDirectorDetails(details.copy(directorMapping = ninosToDirectorsMap(details, ninos)), regId)
    } yield outcome
  }
}
