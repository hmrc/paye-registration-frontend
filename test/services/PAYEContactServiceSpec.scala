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

import enums.{CacheKeys, DownstreamOutcome}
import helpers.PayeComponentSpec
import models.api.{PAYEContact => PAYEContactAPI}
import models.view.{PAYEContactDetails, CompanyDetails => CompanyDetailsView, PAYEContact => PAYEContactView}
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.Format
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PAYEContactServiceSpec extends PayeComponentSpec {
  val returnHttpResponse = HttpResponse(200, "")

  class Setup {
    val service = new PAYEContactService(
      mockPAYERegConnector,
      mockS4LService,
      mockCompanyDetailsService,
      mockPrepopulationService,
      mockAuditService
    )(global)
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
      service.viewToAPI(viewModel) mustBe Right(apiModel)
    }

    "return an incomplete view model when there is no correspondence address" in new Setup {

      val viewModel = PAYEContactView(
        contactDetails = Some(tstContactDetails),
        correspondenceAddress = None
      )
      service.viewToAPI(viewModel) mustBe Left(viewModel)
    }

    "return an incomplete view model when there is no contact details" in new Setup {

      val viewModel = PAYEContactView(
        contactDetails = None,
        correspondenceAddress = Some(tstCorrespondenceAddress)
      )
      service.viewToAPI(viewModel) mustBe Left(viewModel)
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

      service.apiToView(apiModel) mustBe viewModel
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

    val ppobAddress = Address(
      line1 = "PPOB tst line 1",
      line2 = "PPOB tst line 2",
      line3 = None,
      line4 = None,
      postCode = Some("PP1 1OB")
    )

    val detailsWithROAddressForAll = CompanyDetailsView(
      companyName = "Tst Company",
      tradingName = None,
      roAddress = roAddress,
      ppobAddress = Some(roAddress),
      businessContactDetails = None
    )

    val detailsWithROAddressOnly = CompanyDetailsView(
      companyName = "Tst Company",
      tradingName = None,
      roAddress = roAddress,
      ppobAddress = None,
      businessContactDetails = None
    )

    val detailsWithROAndPPOBAddress = CompanyDetailsView(
      companyName = "Tst Company",
      tradingName = None,
      roAddress = roAddress,
      ppobAddress = Some(ppobAddress),
      businessContactDetails = None
    )

    "return a map with ro address only when there is no correspondence address and ppob is equal to ro address" in new Setup {
      service.getCorrespondenceAddresses(None, detailsWithROAddressForAll) mustBe Map("ro" -> roAddress)
    }

    "return a map with ro address only when there is no correspondence address and ppob is None" in new Setup {
      service.getCorrespondenceAddresses(None, detailsWithROAddressOnly) mustBe Map("ro" -> roAddress)
    }

    "return a map with ro and ppob addresses when there is no correspondence address" in new Setup {
      service.getCorrespondenceAddresses(None, detailsWithROAndPPOBAddress) mustBe Map("ro" -> roAddress, "ppob" -> ppobAddress)
    }

    "return a map with ro, ppob and correspondence addresses when all addresses are different" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAndPPOBAddress)
        .mustBe(Map("ro" -> roAddress, "correspondence" -> tstCorrespondenceAddress, "ppob" -> ppobAddress))
    }

    "return a map with ro and correspondence addresses when all addresses are different and ppob is None" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAddressOnly)
        .mustBe(Map("ro" -> roAddress, "correspondence" -> tstCorrespondenceAddress))
    }

    "return a map with correspondence address when all addresses are the same" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAddressForAll.copy(roAddress = tstCorrespondenceAddress, ppobAddress = Some(tstCorrespondenceAddress)))
        .mustBe(Map("correspondence" -> tstCorrespondenceAddress))
    }

    "return a map with ro and correspondence addresses when correspondence and ppob addresses are the same" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAndPPOBAddress.copy(ppobAddress = Some(tstCorrespondenceAddress)))
        .mustBe(Map("ro" -> roAddress, "correspondence" -> tstCorrespondenceAddress))
    }

    "return a map with ppob and correspondence addresses when correspondence and ro addresses are the same" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAndPPOBAddress.copy(roAddress = tstCorrespondenceAddress))
        .mustBe(Map("correspondence" -> tstCorrespondenceAddress, "ppob" -> ppobAddress))
    }

    "return a map with correspondence address when correspondence and ro addresses are the same and ppob is None" in new Setup {
      service.getCorrespondenceAddresses(Some(tstCorrespondenceAddress), detailsWithROAddressOnly.copy(roAddress = tstCorrespondenceAddress))
        .mustBe(Map("correspondence" -> tstCorrespondenceAddress))
    }
  }


  "Calling getPAYEContact" should {
    "return the correct View response when PAYE Contact are returned from s4l" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(Fixtures.validPAYEContactView)))

      await(service.getPAYEContact(testRegId)) mustBe Fixtures.validPAYEContactView
    }

    "return the correct View response when PAYE Contact are returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(Fixtures.validPAYEContactAPI)))

      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyString())
        (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.getPAYEContact(testRegId)) mustBe Fixtures.validPAYEContactView
    }

    "throw an UpstreamErrorResponse when a 403 response is returned from the connector" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("403", 403, 403)))

      an[UpstreamErrorResponse] mustBe thrownBy(await(service.getPAYEContact(testRegId)))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException))

      an[Exception] mustBe thrownBy(await(service.getPAYEContact(testRegId)))
    }

    "return the correct View response when PAYE Contact are returned from Prepopulation" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockPrepopulationService.getPAYEContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Fixtures.validPAYEContactView.contactDetails))

      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyString())
        (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.getPAYEContact(testRegId)) mustBe Fixtures.validPAYEContactView.copy(correspondenceAddress = None)
    }

    "return None when no PAYE Contact are returned from the connector and Prepopulation" in new Setup {
      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      when(mockPAYERegConnector.getPAYEContact(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockPrepopulationService.getPAYEContactDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyString())
        (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.getPAYEContact(testRegId)) mustBe Fixtures.emptyPAYEContactView
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
      when(mockPAYERegConnector.upsertPAYEContact(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYEContactAPI))
      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.submitPAYEContact(PAYEContactView(Some(tstContactDetails), Some(tstCorrespondenceAddress)), "54321")).
        mustBe(DownstreamOutcome.Success)
    }

    "return a success response when successfully saving to S4L" in new Setup {
      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())
        (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      await(service.submitPAYEContact(PAYEContactView(Some(tstContactDetails), None), "54321")).
        mustBe(DownstreamOutcome.Success)
    }
  }

  "dataChanged" should {
    val viewData = PAYEContactDetails(
      name = "testName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("test@email.com"),
        phoneNumber = Some("1234567890"),
        mobileNumber = Some("1234567890")
      )
    )

    val s4lData = PAYEContactDetails(
      name = "testName",
      digitalContactDetails = DigitalContactDetails(
        email = Some("test@email.com"),
        phoneNumber = Some("1234567890"),
        mobileNumber = Some("1234567890")
      )
    )

    "return true" when {
      "s4lData is not defined" in new Setup {
        val dataChanged = service.dataHasChanged(viewData, None)
        dataChanged mustBe true
      }

      "the data sets don't match (name)" in new Setup {
        val changedData = viewData.copy(name = "testName1")
        val dataChanged = service.dataHasChanged(changedData, Some(s4lData))
        dataChanged mustBe true
      }

      "the data sets don't match (email)" in new Setup {
        val changedData = viewData.copy(digitalContactDetails = viewData.digitalContactDetails.copy(email = Some("test1@email.com")))
        val dataChanged = service.dataHasChanged(changedData, Some(s4lData))
        dataChanged mustBe true
      }

      "the data sets don't match (phone)" in new Setup {
        val changedData = viewData.copy(digitalContactDetails = viewData.digitalContactDetails.copy(phoneNumber = Some("0987766454321")))
        val dataChanged = service.dataHasChanged(changedData, Some(s4lData))
        dataChanged mustBe true
      }

      "the data sets don't match (mobile)" in new Setup {
        val changedData = viewData.copy(digitalContactDetails = viewData.digitalContactDetails.copy(mobileNumber = Some("0987766454321")))
        val dataChanged = service.dataHasChanged(changedData, Some(s4lData))
        dataChanged mustBe true
      }

      "the data sets don't match" in new Setup {
        val changedData = viewData.copy(
          name = "testNewName",
          digitalContactDetails = viewData.digitalContactDetails.copy(
            email = Some("newTest@email.com"),
            phoneNumber = Some("0987766454321"),
            mobileNumber = Some("124134")
          )
        )
        val dataChanged = service.dataHasChanged(changedData, Some(s4lData))
        dataChanged mustBe true
      }

      "the data sets don't match (contains Nones)" in new Setup {
        val changedData = viewData.copy(
          name = "testNewName",
          digitalContactDetails = viewData.digitalContactDetails.copy(
            email = None,
            phoneNumber = Some("0987766454321"),
            mobileNumber = None
          )
        )
        val dataChanged = service.dataHasChanged(changedData, Some(s4lData))
        dataChanged mustBe true
      }
    }

    "return false" when {
      "both data sets match" in new Setup {
        val dataChanged = service.dataHasChanged(viewData, Some(s4lData))
        dataChanged mustBe false
      }

      "data sets contain spaces but the same data" in new Setup {
        val changedData = viewData.copy(
          name = "test Name",
          digitalContactDetails = viewData.digitalContactDetails.copy(
            email = Some("test@ email.com"),
            phoneNumber = Some("123 456 7890"),
            mobileNumber = Some("1234 56789 0")
          )
        )
        val dataChanged = service.dataHasChanged(service.flattenData(changedData), Some(s4lData))
        dataChanged mustBe false
      }
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

    "save a copy of paye contact (no audit)" in new Setup {
      implicit val request = FakeRequest()

      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(PAYEContactView(Some(tstContactDetails), None))))

      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      when(mockPrepopulationService.saveContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(tstContactDetails))

      await(service.submitPayeContactDetails("12345", tstContactDetails)) mustBe DownstreamOutcome.Success
    }

    "save a copy of paye contact" in new Setup {
      implicit val request = FakeRequest()

      when(mockS4LService.fetchAndGet[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[PAYEContactView]]()))
        .thenReturn(Future.successful(Some(PAYEContactView(None, None))))

      when(mockS4LService.saveForm[PAYEContactView](ArgumentMatchers.contains(CacheKeys.PAYEContact.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      when(mockAuditService.auditPAYEContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))
        .thenReturn(Future.successful(AuditResult.Success))

      when(mockPrepopulationService.saveContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(tstContactDetails))

      await(service.submitPayeContactDetails("12345", tstContactDetails)) mustBe DownstreamOutcome.Success
    }
  }

  "Calling saveCorrespondenceAddress" should {

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

      await(service.submitCorrespondence("54321", tstCorrespondenceAddress)) mustBe DownstreamOutcome.Success
    }
  }
}
