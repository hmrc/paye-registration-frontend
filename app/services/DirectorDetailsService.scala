/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AppConfig
import connectors.PAYERegistrationConnector
import enums.{CacheKeys, DownstreamOutcome}
import javax.inject.Inject
import models.api.Director
import models.view.{Directors, Ninos, UserEnteredNino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.RegistrationWhitelist

import scala.concurrent.Future

class DirectorDetailsServiceImpl @Inject()(val payeRegConnector: PAYERegistrationConnector,
                                           val incorpInfoService: IncorporationInformationService,
                                           val s4LService: S4LService
                                          )(implicit val appConfig: AppConfig) extends DirectorDetailsService

trait DirectorDetailsService extends RegistrationWhitelist {
  implicit val appConfig: AppConfig
  val payeRegConnector: PAYERegistrationConnector
  val s4LService: S4LService
  val incorpInfoService: IncorporationInformationService

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


  private def saveToS4L(viewData: Directors, regId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    s4LService.saveForm[Directors](CacheKeys.DirectorDetails.toString, viewData, regId).map(_ => viewData)
  }

  def getDirectorDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    for {
      iiDirectors <- incorpInfoService.getDirectorDetails(txId = transactionId, regId)
      backendDirectors <- s4LService.fetchAndGet(CacheKeys.DirectorDetails.toString, regId) flatMap {
        case Some(dirs) => Future.successful(dirs)
        case None => for {
          regResponse <- ifRegIdNotWhitelisted(regId) {
            payeRegConnector.getDirectors(regId)
          }
        } yield {
          apiToView(regResponse)
        }
      }
      directors = if (directorsNotChanged(iiDirectors, backendDirectors)) backendDirectors else iiDirectors
      _ <- saveToS4L(directors, regId)
    } yield {
      directors
    }
  }

  private[services] def saveDirectorDetails(viewModel: Directors, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel) fold(
      incompleteView =>
        saveToS4L(incompleteView, regId) map { _ => DownstreamOutcome.Success },
      completeAPI =>
        for {
          details <- payeRegConnector.upsertDirectors(regId, completeAPI)
          clearData <- s4LService.clear(regId)
        } yield DownstreamOutcome.Success
    )
  }


  def createDisplayNamesMap(directors: Directors): Map[String, String] = {
    directors.directorMapping.map {
      case (k, v) => (k, List(v.name.title, v.name.forename, v.name.otherForenames, Some(v.name.surname)).flatten.mkString(" "))
    }
  }


  def directorsNotChanged(iiDirectors: Directors, backendDirectors: Directors): Boolean = {
    val numberOfDirectorsAreTheSame = iiDirectors.directorMapping.seq.size == backendDirectors.directorMapping.seq.size

    ! {
      if (numberOfDirectorsAreTheSame) {
        iiDirectors.directorMapping.values.map { ii =>
          backendDirectors.directorMapping.values.exists(_.name == ii.name)
        }.toList
      } else {
        List(false)
      }
    }.contains(false)
  }


  def createDirectorNinos(directors: Directors): Ninos = {
    Ninos((0 until directors.directorMapping.size).map {
      index =>
        UserEnteredNino(
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
