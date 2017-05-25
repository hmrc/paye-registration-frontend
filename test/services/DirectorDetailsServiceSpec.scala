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

import connectors.{PAYERegistrationConnect, PAYERegistrationConnector}
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.{CoHoAPIFixture, PAYERegistrationFixture, S4LFixture}
import models.api.{Director, Name}
import models.view.{Directors, Ninos, UserEnteredNino}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{Format, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class DirectorDetailsServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture with CoHoAPIFixture {

  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockCoHoService = mock[IncorporationInformationService]
  val mockS4LService = mock[S4LService]

  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new DirectorDetailsSrv {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockCoHoService
      override val s4LService = mockS4LService
    }
  }

  class NoDirectorDetailsMockedSetup {
    val service = new DirectorDetailsSrv {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockCoHoService
      override val s4LService = mockS4LService

      override def getDirectorDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
        Future.successful(validDirectorDetailsViewModel)
      }

      override def saveDirectorDetails(detailsView: Directors, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Failure)
      }
    }
  }

  class DirectorDetailsMockedSetup {
    val service = new DirectorDetailsSrv {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockCoHoService
      override val s4LService = mockS4LService

      override def getDirectorDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
        Future.successful(validDirectorDetailsViewModel)
      }

      override def saveDirectorDetails(detailsView: Directors, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Success)
      }
    }
  }

  class APIConverterMockedSetup {
    val service = new DirectorDetailsSrv {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockCoHoService
      override val s4LService = mockS4LService

      override def apiToView(apiModel: Seq[Director]): Directors = {
        validDirectorDetailsViewModel
      }
    }
  }

  "Calling apiToView" should {
    "correctly produce a view model from a list of Director API model" in new Setup {
      val tstModelAPI = List(
        Director(
          name = Name(
            forename = Some("Timothy"),
            otherForenames = Some("Potterley-Smythe"),
            surname = "Buttersford",
            title = Some("Mr")
          ),
          nino = Some("ZZ123456A")
        ),
        Director(
          name = Name(
            forename = Some("Peter"),
            otherForenames = Some("Pierre"),
            surname = "Simpson",
            title = Some("Sir")
          ),
          nino = None
        )
      )
      val tstModelView = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("Timothy"), Some("Potterley-Smythe"), "Buttersford", Some("Mr")),
            nino = Some("ZZ123456A")
          ),
          "1" -> Director(
            name = Name(Some("Peter"), Some("Pierre"), "Simpson", Some("Sir")),
            nino = None
          )
        )
      )

      service.apiToView(tstModelAPI) shouldBe tstModelView
    }
  }

  "Calling viewToAPI" should {
    "correctly produce a Directors API model from a completed view model" in new Setup {
      val tstModelAPI = List(
        Director(
          name = Name(
            forename = Some("Timothy"),
            otherForenames = Some("Potterley-Smythe"),
            surname = "Buttersford",
            title = Some("Mr")
          ),
          nino = Some("ZZ123456A")
        ),
        Director(
          name = Name(
            forename = Some("Peter"),
            otherForenames = Some("Pierre"),
            surname = "Simpson",
            title = Some("Sir")
          ),
          nino = None
        )
      )

      val tstModelView = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("Timothy"), Some("Potterley-Smythe"), "Buttersford", Some("Mr")),
            nino = Some("ZZ123456A")
          ),
          "1" -> Director(
            name = Name(Some("Peter"), Some("Pierre"), "Simpson", Some("Sir")),
            nino = None
          )
        )
      )

      service.viewToAPI(tstModelView) shouldBe Right(tstModelAPI)
    }
  }

  "Creating view models" when {

    val tstDirectors = Directors(
      directorMapping = Map(
        "0" -> Director(
          name = Name(Some("Timothy"), Some("Potterley-Smythe"), "Buttersford", Some("Mr")),
          nino = Some("ZZ123456A")
        ),
        "1" -> Director(
          name = Name(Some("Peter"), Some("Pierre"), "Simpson", Some("Sir")),
          nino = None
        )
      )
    )

    "Calling createDisplayNamesMap" should {
      "correctly produce a map of IDs to names from a completed view model" in new Setup {

        val displayMap = Map(
            "0" -> "Timothy Buttersford",
            "1" -> "Peter Simpson"
          )

        service.createDisplayNamesMap(tstDirectors) shouldBe displayMap
      }
    }

    "Calling createDirectorNinos" should {
      "correctly produce a mapping of ninos to IDs from a completed view model" in new Setup {

        val ninos = Ninos(ninoMapping = List(
            UserEnteredNino("0", Some("ZZ123456A")),
            UserEnteredNino("1", None)
          ))


        service.createDirectorNinos(tstDirectors) shouldBe ninos
      }
    }
  }

  "Calling ninosToDirectorsMap" should {
    "correctly produce a Directors map from a Ninos model" in new APIConverterMockedSetup {
      val validNinos = Ninos(
        List(
          UserEnteredNino("0", Some("AA123456Z")),
          UserEnteredNino("1", Some("ZZ123456A"))
        )
      )

      val expectedDirectorDetailsViewModel = Map(
        "0" -> Director(
          name = Name(Some("Bob"), None, "Smith", None),
          nino = Some("AA123456Z")
        ),
        "1" -> Director(
          name = Name(Some("Michael"), Some("Jay"), "Fudgedybar", None),
          nino = Some("ZZ123456A")
        )
      )

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(Some(validDirectorDetailsViewModel)))

      await(service.ninosToDirectorsMap(validDirectorDetailsViewModel, validNinos)) shouldBe expectedDirectorDetailsViewModel
    }
  }

  "Calling getDirectorDetails" should {
    "return the correct View response when Director Details are returned from S4L" in new Setup {

      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("test2"), Some("test22"), "testb", Some("Mr")),
            nino = None
          )
        )
      )

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(Some(directorDetails)))

      await(service.getDirectorDetails("12345", "txId")) shouldBe directorDetails
    }

    "return the correct View response when Director Details are returned from the microservice" in new Setup {

      val dir = Director(
        name = Name(Some("test2"), Some("test22"), "testb", Some("Mr")),
        nino = None
      )

      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> dir
        )
      )

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Seq(dir)))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) shouldBe directorDetails
    }

    "return the correct View response when Director Details are returned from the CoHo service" in new Setup {

      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("test2"), Some("test22"), "testb", Some("Mr")),
            nino = None
          )
        )
      )
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Nil))

      when(mockCoHoService.getDirectorDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(directorDetails))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) shouldBe directorDetails
    }

    "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

      an[Upstream4xxResponse] shouldBe thrownBy(await(service.getDirectorDetails("12345", "txId")))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

      an[Exception] shouldBe thrownBy(await(service.getDirectorDetails("12345", "txId")))
    }
  }

  "Calling saveDirectorDetails" should {
    "return a success response when the upsert completes successfully" in new Setup {
      when(mockPAYERegConnector.upsertDirectors(ArgumentMatchers.contains("12345"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validDirectorList))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveDirectorDetails(validDirectorDetailsViewModel, "12345")) shouldBe DownstreamOutcome.Success
    }

    "return a success response when the S4L save completes successfully" in new Setup {
      val incompleteCompanyDetailsViewModel = Directors(directorMapping = Map())

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.saveDirectorDetails(incompleteCompanyDetailsViewModel, "12345")) shouldBe DownstreamOutcome.Success
    }
  }

  "Calling submitNinos" should {
    "return a success response when submit is completed successfully" in new DirectorDetailsMockedSetup {
      await(service.submitNinos(validNinos, "54322", "txId")) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoDirectorDetailsMockedSetup {
      await(service.submitNinos(validNinos, "54322", "txId")) shouldBe DownstreamOutcome.Failure
    }
  }
}
