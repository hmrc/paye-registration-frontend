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

import javax.inject.Inject

import connectors.{KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import enums.{CacheKeys, DownstreamOutcome}
import models.api.Director
import models.view.{Directors, Ninos, UserEnteredNino}
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DirectorDetailsService @Inject()(
                                        keystoreConn: KeystoreConnector,
                                        payeRegistrationConn: PAYERegistrationConnector,
                                        coHoAPIServ: CoHoAPIService,
                                        s4LServ: S4LService) extends DirectorDetailsSrv {
  override val keystoreConnector = keystoreConn
  override val payeRegConnector = payeRegistrationConn
  override val coHoAPIService = coHoAPIServ
  override val s4LService = s4LServ
}

trait DirectorDetailsSrv extends CommonService {
  val payeRegConnector: PAYERegistrationConnect
  val s4LService: S4LSrv
  val coHoAPIService: CoHoAPISrv

  implicit val formatRecordSet = Json.format[Directors]

  private[services] def directorsToTupleView(directors: Directors): Future[(Map[String, String], Ninos)] = {
    val directorsName = directors.directorMapping.map {
      case(k, v) => (k, List(v.name.forename, v.name.surname).flatten.mkString(" "))
    }
    val ninos = Ninos(ninoMapping = directors.directorMapping.toList.map (list => UserEnteredNino(list._1, list._2.nino)))

    Future.successful((directorsName, ninos))
  }

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

  private[services] def convertOrRetrieveDirectors(directorList: Seq[Director])(implicit hc: HeaderCarrier): Future[Directors] = {
    directorList match {
      case Nil => for {
        directors <- coHoAPIService.getDirectorDetails()
      } yield directors
      case dirList => Future.successful(apiToView(dirList))
    }
  }

  private def saveToS4L(viewData: Directors)(implicit hc: HeaderCarrier): Future[Directors] = {
    s4LService.saveForm[Directors](CacheKeys.DirectorDetails.toString, viewData).map(_ => viewData)
  }

  def getDirectorDetails()(implicit hc: HeaderCarrier): Future[Directors] = {
    s4LService.fetchAndGet(CacheKeys.DirectorDetails.toString) flatMap {
      case Some(directors) => Future.successful(directors)
      case None => for {
        regID <- fetchRegistrationID
        regResponse <- payeRegConnector.getDirectors(regID)
        directors <- convertOrRetrieveDirectors(regResponse)
        data <- saveToS4L(directors)
      } yield data
    }
  }

  private[services] def saveDirectorDetails(viewModel: Directors)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel) fold(
      incompleteView =>
        saveToS4L(incompleteView) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          regID     <- fetchRegistrationID
          details   <- payeRegConnector.upsertDirectors(regID, completeAPI)
          clearData <- s4LService.clear
        } yield DownstreamOutcome.Success
    )
  }

  def getNinos()(implicit hc: HeaderCarrier): Future[(Map[String, String], Ninos)] = {
    for {
      directors <- getDirectorDetails
      viewData <- directorsToTupleView(directors)
    } yield viewData
  }

  def submitNinos(ninos: Ninos)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getDirectorDetails
      outcome <- saveDirectorDetails(details.copy(directorMapping = ninosToDirectorsMap(details, ninos)))
    } yield outcome
  }
}
