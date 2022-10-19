/*
 * Copyright 2022 HM Revenue & Customs
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
import enums.{CacheKeys, DownstreamOutcome}
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.api.{Director, Name}
import models.view.{Directors, Ninos, UserEnteredNino}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class DirectorDetailsServiceSpec extends PayeComponentSpec with PayeFakedApp {
  val returnHttpResponse = HttpResponse(200, "")

  class Setup {
    val service = new DirectorDetailsService {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockIncorpInfoService
      override val s4LService = mockS4LService
      override implicit val appConfig: AppConfig = injAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }

  class NoDirectorDetailsMockedSetup {
    val service = new DirectorDetailsService {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockIncorpInfoService
      override val s4LService = mockS4LService
      override implicit val appConfig: AppConfig = injAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global


      override def getDirectorDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
        Future.successful(Fixtures.validDirectorDetailsViewModel)
      }

      override def saveDirectorDetails(detailsView: Directors, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Failure)
      }
    }
  }

  class DirectorDetailsMockedSetup {
    val service = new DirectorDetailsService {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockIncorpInfoService
      override val s4LService = mockS4LService
      override implicit val appConfig: AppConfig = injAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global


      override def getDirectorDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
        Future.successful(Fixtures.validDirectorDetailsViewModel)
      }

      override def saveDirectorDetails(detailsView: Directors, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
        Future.successful(DownstreamOutcome.Success)
      }
    }
  }

  class APIConverterMockedSetup {
    val service = new DirectorDetailsService {
      override val payeRegConnector = mockPAYERegConnector
      override val incorpInfoService = mockIncorpInfoService
      override val s4LService = mockS4LService
      override implicit val appConfig: AppConfig = injAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global


      override def apiToView(apiModel: Seq[Director]): Directors = {
        Fixtures.validDirectorDetailsViewModel
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

      service.apiToView(tstModelAPI) mustBe tstModelView
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

      service.viewToAPI(tstModelView) mustBe Right(tstModelAPI)
    }
  }

  "Creating view models" when {

    val tstDirectors = Directors(
      directorMapping = Map(
        "0" -> Director(
          name = Name(Some("Timothy"), Some("Potterley-Smythe"), Some("Buttersford"), Some("Mr")),
          nino = Some("ZZ123456A")
        ),
        "1" -> Director(
          name = Name(Some("Peter"), None, Some("Simpson"), Some("Sir")),
          nino = None
        )
      )
    )

    "Calling createDisplayNamesMap" should {
      "correctly produce a map of IDs to names from a completed view model" in new Setup {

        val displayMap = Map(
          "0" -> "Mr Timothy Potterley-Smythe Buttersford",
          "1" -> "Sir Peter Simpson"
        )

        service.createDisplayNamesMap(tstDirectors) mustBe displayMap
      }
    }

    "Calling createDirectorNinos" should {
      "correctly produce a mapping of ninos to IDs from a completed view model" in new Setup {

        val ninos = Ninos(ninoMapping = List(
          UserEnteredNino("0", Some("ZZ123456A")),
          UserEnteredNino("1", None)
        ))


        service.createDirectorNinos(tstDirectors) mustBe ninos
      }
    }
  }

  "areDirectorsUnchanged" should {
    "return true" when {
      "the II mapping is the same as the backend mapping" in new Setup {
        val iiDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe true
      }
      "the II mapping is the same as the backend mapping but the order of directors is different" in new Setup {
        val iiDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None),
            "1" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None)

          )
        )
        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe true
      }
    }


    "return false" when {
      "the II mapping is different to the backend mapping" in new Setup {
        val iiDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("NewTitle")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), Some("NewTitle")), None)
          )
        )

        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe false
      }

      "the II mapping is different (less elements in backend map" in new Setup {
        val iiDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )
        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("NewTitle")), None)
          )
        )

        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe false
      }
      "the II mapping is different whereby the casing is different for title" in new Setup {
        val iiDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )
        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("Title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe false

      }
      "the ii mapping is different whereby coho has less elements" in new Setup {
        val iiDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("title")), None)
          )
        )
        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("Title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe false
      }
      "the ii mapping has no elements" in new Setup {
        val iiDirectors = Directors(
          Map()
        )
        val backendDirectors = Directors(
          Map(
            "0" -> Director(Name(Some("first"), Some("middle"), Some("last"), Some("Title")), None),
            "1" -> Director(Name(Some("first1"), Some("middle1"), Some("last1"), None), None)
          )
        )

        service.directorsNotChanged(iiDirectors, backendDirectors) mustBe false
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
          name = Name(Some("Bob"), None, Some("Smith"), None),
          nino = Some("AA123456Z")
        ),
        "1" -> Director(
          name = Name(Some("Michael"), Some("Jay"), Some("Fudgedybar"), None),
          nino = Some("ZZ123456A")
        )
      )

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(Some(Fixtures.validDirectorDetailsViewModel)))

      service.ninosToDirectorsMap(Fixtures.validDirectorDetailsViewModel, validNinos) mustBe expectedDirectorDetailsViewModel
    }
  }

  "Calling getDirectorDetails" should {
    "return the correct View response when Director Details are returned from S4L" in new Setup {

      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
            nino = None
          )
        )
      )

      when(mockIncorpInfoService.getDirectorDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(directorDetails))

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(Some(directorDetails)))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) mustBe directorDetails
    }

    "return coho directors when paye reg directors do not match" in new Setup {

      def dir(nino: Option[String], title: Option[String]) = Director(
        name = Name(Some("test2"), Some("test22"), Some("testb"), title),
        nino = nino)

      val cohoDirectors = Directors(
        directorMapping = Map(
          "0" -> dir(nino = Some("foo"), title = Some("title1"))
        )
      )
      val payeregDirectors = Seq(dir(nino = None, title = Some("Title2"))

      )
      when(mockIncorpInfoService.getDirectorDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(cohoDirectors))

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(payeregDirectors))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) mustBe cohoDirectors

    }

    "return coho directors when s4l directors do not match" in new Setup {
      def dir(nino: Option[String], title: Option[String]) = Director(
        name = Name(Some("test2"), Some("test22"), Some("testb"), title),
        nino = nino)

      val cohoDirectors = Directors(
        directorMapping = Map(
          "0" -> dir(nino = Some("foo"), title = Some("title1"))
        )
      )
      val s4lDirectors = Directors(
        directorMapping = Map(
          "0" -> dir(nino = None, title = Some("Title2"))
        )
      )
      when(mockIncorpInfoService.getDirectorDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(cohoDirectors))

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(Some(s4lDirectors)))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) mustBe cohoDirectors
    }

    "return the correct View response when Director Details are returned from the microservice" in new Setup {

      val dir = Director(
        name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
        nino = None
      )

      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> dir
        )
      )

      when(mockIncorpInfoService.getDirectorDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(directorDetails))

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Seq(dir)))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) mustBe directorDetails
    }

    "return the correct View response when Director Details are returned from the CoHo service" in new Setup {

      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
            nino = None
          )
        )
      )
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Nil))

      when(mockIncorpInfoService.getDirectorDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(directorDetails))

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.getDirectorDetails("12345", "txId")) mustBe directorDetails
    }

    "throw an UpstreamErrorResponse when a 403 response is returned from the connector" in new Setup {
      val directorDetails = Directors(
        directorMapping = Map(
          "0" -> Director(
            name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
            nino = None
          )
        )
      )

      when(mockIncorpInfoService.getDirectorDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(directorDetails))

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("403", 403, 403)))

      an[UpstreamErrorResponse] mustBe thrownBy(await(service.getDirectorDetails("12345", "txId")))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getDirectors(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

      an[Exception] mustBe thrownBy(await(service.getDirectorDetails("12345", "txId")))
    }
  }

  "Calling saveDirectorDetails" should {
    "return a success response when the upsert completes successfully" in new Setup {
      when(mockPAYERegConnector.upsertDirectors(ArgumentMatchers.contains("12345"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validDirectorList))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveDirectorDetails(Fixtures.validDirectorDetailsViewModel, "12345")) mustBe DownstreamOutcome.Success
    }

    "return a success response when the S4L save completes successfully" in new Setup {
      val incompleteCompanyDetailsViewModel = Directors(directorMapping = Map())

      when(mockS4LService.saveForm(ArgumentMatchers.eq(CacheKeys.DirectorDetails.toString), ArgumentMatchers.any, ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Directors]]()))
        .thenReturn(Future.successful(CacheMap("", Map("" -> Json.toJson("")))))

      await(service.saveDirectorDetails(incompleteCompanyDetailsViewModel, "12345")) mustBe DownstreamOutcome.Success
    }
  }

  "Calling submitNinos" should {
    "return a success response when submit is completed successfully" in new DirectorDetailsMockedSetup {
      await(service.submitNinos(Fixtures.validNinos, "54322", "txId")) mustBe DownstreamOutcome.Success
    }

    "return a failure response when submit is not completed successfully" in new NoDirectorDetailsMockedSetup {
      await(service.submitNinos(Fixtures.validNinos, "54322", "txId")) mustBe DownstreamOutcome.Failure
    }
  }
}
