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

import connectors.{CoHoAPIConnector, CompanyRegistrationConnector, PAYERegistrationConnector}
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.{CoHoAPIFixture, PAYERegistrationFixture, S4LFixture}
import models.api.{Director, Name}
import models.view.Directors
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.libs.json.{Format, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class DirectorDetailsServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture with CoHoAPIFixture {

  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockCoHoService = mock[CoHoAPIService]
  val mockS4LService = mock[S4LService]

  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new DirectorDetailsService(mockKeystoreConnector, mockPAYERegConnector, mockCoHoService, mockS4LService)
  }

  class APIConverterMockedSetup {
    val service = new DirectorDetailsService (mockKeystoreConnector, mockPAYERegConnector, mockCoHoService, mockS4LService) {

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
            surname = Some("Buttersford"),
            title = Some("Mr")
          ),
          nino = Some("ZZ123456A")
        ),
        Director(
          name = Name(
            forename = Some("Peter"),
            otherForenames = Some("Pierre"),
            surname = Some("Simpson"),
            title = Some("Sir")
          ),
          nino = None
        )
      )
      val tstModelView = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("Timothy"), Some("Potterley-Smythe"), Some("Buttersford"), Some("Mr")),
            nino = Some("ZZ123456A")
          ),
          "1" -> Director(
            name = Name(Some("Peter"), Some("Pierre"), Some("Simpson"), Some("Sir")),
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
            surname = Some("Buttersford"),
            title = Some("Mr")
          ),
          nino = Some("ZZ123456A")
        ),
        Director(
          name = Name(
            forename = Some("Peter"),
            otherForenames = Some("Pierre"),
            surname = Some("Simpson"),
            title = Some("Sir")
          ),
          nino = None
        )
      )

      val tstModelView = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("Timothy"), Some("Potterley-Smythe"), Some("Buttersford"), Some("Mr")),
            nino = Some("ZZ123456A")
          ),
          "1" -> Director(
            name = Name(Some("Peter"), Some("Pierre"), Some("Simpson"), Some("Sir")),
            nino = None
          )
        )
      )

      service.viewToAPI(tstModelView) shouldBe Right(tstModelAPI)
    }
  }

  "Calling getDirectorDetails" should {
    "return the correct View response when Director Details are returned from S4L" in new APIConverterMockedSetup {
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.DirectorDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(Some(validDirectorDetailsViewModel)))

      await(service.getDirectorDetails) shouldBe validDirectorDetailsViewModel
    }

    "return the correct View response when Director Details are returned from the connector" in new APIConverterMockedSetup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.DirectorDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(validDirectorList))

      when(mockS4LService.saveForm(Matchers.eq(CacheKeys.DirectorDetails.toString),Matchers.any)(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails) shouldBe validDirectorDetailsViewModel
    }
  }

  "return the correct View response when Director Details are returned from the CoHo service" in new Setup {
    mockFetchRegID("54321")
    when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.DirectorDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
      .thenReturn(Future.successful(None))

    when(mockPAYERegConnector.getDirectors(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Nil))

    when(mockCoHoService.getDirectorDetails()(Matchers.any()))
      .thenReturn(Future.successful(validDirectorDetails))

    when(mockS4LService.saveForm(Matchers.eq(CacheKeys.DirectorDetails.toString),Matchers.any)(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
      .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

    await(service.getDirectorDetails) shouldBe validDirectorDetails
  }

  "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
    mockFetchRegID("54321")
    when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.DirectorDetails.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
      .thenReturn(Future.successful(None))

    when(mockPAYERegConnector.getDirectors(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

    an[Upstream4xxResponse] shouldBe thrownBy(await(service.getDirectorDetails))
  }

  "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
    mockFetchRegID("54321")
    when(mockPAYERegConnector.getDirectors(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

    an[Exception] shouldBe thrownBy(await(service.getDirectorDetails))
  }

  "Calling saveDirectorDetails" should {
    "return a success response when the upsert completes successfully" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.upsertDirectors(Matchers.contains("54321"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(validDirectorList))

      when(mockS4LService.clear()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveDirectorDetails(validDirectorDetailsViewModel)) shouldBe DownstreamOutcome.Success
    }

    "return a success response when the S4L save completes successfully" in new Setup {
      val incompleteCompanyDetailsViewModel = Directors(directorMapping = Map())

      when(mockS4LService.saveForm(Matchers.eq(CacheKeys.DirectorDetails.toString),Matchers.any)(Matchers.any[HeaderCarrier](), Matchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.saveDirectorDetails(incompleteCompanyDetailsViewModel)) shouldBe DownstreamOutcome.Success
    }
  }
}
