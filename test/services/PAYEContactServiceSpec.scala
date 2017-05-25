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

import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.PAYERegistrationFixture
import models.{Address, DigitalContactDetails}
import models.view.{PAYEContact => PAYEContactView, PAYEContactDetails, CompanyDetails => CompanyDetailsView}
import models.api.{PAYEContact => PAYEContactAPI}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.Format
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class PAYEContactServiceSpec extends PAYERegSpec with PAYERegistrationFixture {
  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockCompRegConnector = mock[CompanyRegistrationConnector]
  val mockCohoAPIConnector = mock[IncorporationInformationConnector]
  val mockCoHoService = mock[IncorporationInformationService]
  val mockS4LService = mock[S4LService]

  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new PAYEContactSrv {
      val payeRegConnector = mockPAYERegConnector
      val s4LService = mockS4LService
      val keystoreConnector = mockKeystoreConnector
    }
  }

  val testRegId = "54321"

  "Calling viewToAPI" should {

    val tstContactDetails = PAYEContactDetails(
      name = "tstName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("tst@tst.com"),
        mobileNumber = Some("07754123456"),
        phoneNumber = Some("01214321234")
      )
    )
    val tstCorrespondenceAddress = Address(
      line1 = "tst line 1",
      line2 = "tst line 2",
      line3 = None,
      line4 = None,
      postCode = Some("TE1 1ST")
    )

    "return the correct API model when a full view model is passed" in new Setup {

      val viewModel = PAYEContactView(
        contactDetails = Some(tstContactDetails),
        correspondenceAddress = Some(tstCorrespondenceAddress)
      )
      val apiModel = PAYEContactAPI(
        contactDetails = tstContactDetails,
        correspondenceAddress = tstCorrespondenceAddress
      )
      service.viewToAPI(viewModel) shouldBe Right(apiModel)
    }

    "return an incomplete view model when there is no correspondence address" in new Setup {

      val viewModel = PAYEContactView(
        contactDetails = Some(tstContactDetails),
        correspondenceAddress = None
      )
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }

    "return an incomplete view model when there is no contact details" in new Setup {

      val viewModel = PAYEContactView(
        contactDetails = None,
        correspondenceAddress = Some(tstCorrespondenceAddress)
      )
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "Calling apiToView" should {
    "return a view model" in new Setup {
      val tstContactDetails = PAYEContactDetails(
        name = "tstName",
        digitalContactDetails = DigitalContactDetails(
          email = Some("tst@tst.com"),
          mobileNumber = Some("07754123456"),
          phoneNumber = Some("01214321234")
        )
      )
      val tstCorrespondenceAddress = Address(
        line1 = "tst line 1",
        line2 = "tst line 2",
        line3 = None,
        line4 = None,
        postCode = Some("TE1 1ST")
      )
      val viewModel = PAYEContactView(
        contactDetails = Some(tstContactDetails),
        correspondenceAddress = Some(tstCorrespondenceAddress)
      )
      val apiModel = PAYEContactAPI(
        contactDetails = tstContactDetails,
        correspondenceAddress = tstCorrespondenceAddress
      )

      service.apiToView(apiModel) shouldBe viewModel
    }
  }

  "Calling getCorrespondenceAddresses" should {
    val tstCorrespondenceAddress = Address(
      line1 = "tst line 1",
      line2 = "tst line 2",
      line3 = None,
      line4 = None,
      postCode = Some("TE1 1ST")
    )
    val roAddress = Address(
      line1 = "RO tst line 1",
      line2 = "RO tst line 2",
      line3 = None,
      line4 = None,
      postCode = Some("RO1 1RO")
    )
    val detailsWithROAddress = CompanyDetailsView(
      companyName = "Tst Company",
      tradingName = None,
      roAddress = roAddress,
      ppobAddress = None,
      businessContactDetails = None
    )

    "return a map with ro address when there is no correspondence address" in new Setup {
      service.getCorrespondenceAddresses(None, detailsWithROAddress) shouldBe Map("ro" -> roAddress)
    }

    "return a map with ro and correspondence address when both addresses are different" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAddress)
        .shouldBe(Map("ro" -> roAddress, "correspondence" -> tstCorrespondenceAddress))
    }

    "return a map with correspondence address when both addresses are the same" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAddress.copy(roAddress = tstCorrespondenceAddress))
        .shouldBe(Map("correspondence" -> tstCorrespondenceAddress))
    }
  }


  "Calling getPAYEContact" should {
    "return the correct View response when PAYE Contact are returned from s4l" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(validPAYEContactView)))

      await(service.getPAYEContact(testRegId)) shouldBe validPAYEContactView
    }
    "return the correct View response when PAYE Contact are returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(validPAYEContactAPI)))

      await(service.getPAYEContact(testRegId)) shouldBe validPAYEContactView
    }

    "return None when no PAYE Contact are returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))

      await(service.getPAYEContact(testRegId)) shouldBe emptyPAYEContactView
    }

    "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

      an[Upstream4xxResponse] shouldBe thrownBy(await(service.getPAYEContact(testRegId)))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(new RuntimeException))

      an[Exception] shouldBe thrownBy(await(service.getPAYEContact(testRegId)))
    }

  }

  "Calling submitPAYEContact" should {

    val tstContactDetails = PAYEContactDetails(
      name = "tstName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("tst@tst.com"),
        mobileNumber = Some("07754123456"),
        phoneNumber = Some("01214321234")
      )
    )
    val tstCorrespondenceAddress = Address(
      line1 = "tst line 1",
      line2 = "tst line 2",
      line3 = None,
      line4 = None,
      postCode = Some("TE1 1ST")
    )

    "return a success response when the upsert completes successfully" in new Setup {
      when(mockPAYERegConnector.upsertPAYEContact(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validPAYEContactAPI))
      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.submitPAYEContact(PAYEContactView(Some(tstContactDetails), Some(tstCorrespondenceAddress)), "54321")).
        shouldBe(DownstreamOutcome.Success)
    }

    "return a success response when successfully saving to S4L" in new Setup {
      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())
        (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.submitPAYEContact(PAYEContactView(Some(tstContactDetails), None), "54321")).
        shouldBe(DownstreamOutcome.Success)
    }
  }

  "Calling submitPAYEContactDetails" should {

    val tstContactDetails = PAYEContactDetails(
      name = "tstName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("tst@tst.com"),
        mobileNumber = Some("07754123456"),
        phoneNumber = Some("01214321234")
      )
    )

    "save a copy of paye contact" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(Some(PAYEContactView(None, None))))
      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.submitPayeContactDetails(tstContactDetails, "54321")) shouldBe DownstreamOutcome.Success
    }
  }

  "Calling submitCorrespondence" should {

    val tstCorrespondenceAddress = Address(
      line1 = "tst line 1",
      line2 = "tst line 2",
      line3 = None,
      line4 = None,
      postCode = Some("TE1 1ST")
    )

    "save a copy of paye contact" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(Some(PAYEContactView(None, None))))
      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.submitCorrespondence(tstCorrespondenceAddress, "54321")) shouldBe DownstreamOutcome.Success
    }
  }
}
