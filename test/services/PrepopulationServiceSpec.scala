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

import common.exceptions.DownstreamExceptions.S4LFetchException
import models.Address
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Format
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PrepopulationServiceSpec extends PAYERegSpec {
  implicit val hc = HeaderCarrier()
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new PrepopulationSrv {
      override val busRegConnector = mockBusinessRegistrationConnector
      override val s4LService = mockS4LService
    }
  }

  val addr1 = Address(
    line1 = "line 1",
    line2 = "line 2",
    line3 = Some("line 3"),
    line4 = Some("line 4"),
    postCode = Some("TE1 1ST"),
    country = None,
    auditRef = Some("tstAuditRef")
  )

  val addr2 = Address(
    line1 = "line one",
    line2 = "line two",
    line3 = None,
    line4 = None,
    postCode = Some("TE1 2ST"),
    country = None
  )

  val addr3 = Address(
    line1 = "line",
    line2 = "other line",
    line3 = Some("line 3"),
    line4 = Some("line 4"),
    postCode = None,
    country = Some("UK"),
    auditRef = Some("tstAuditRef2")
  )

  "FilterAddresses" should {
    "return an address map when no duplicates and RO is different" in new Setup {
      val addresses = Seq(addr1, addr2)
      val resMap = Map(0 -> addr1, 1 -> addr2)
      val roAddress = addr3
      val otherAddressOpt = None
      service.filterAddresses(addresses, roAddress, otherAddressOpt) shouldBe resMap
    }

    "filter out duplicates" in new Setup {
      val addresses = Seq(addr1, addr2, addr1)
      val resMap = Map(0 -> addr1, 1 -> addr2)
      val roAddress = addr3
      val otherAddressOpt = None
      service.filterAddresses(addresses, roAddress, otherAddressOpt) shouldBe resMap
    }

    "filter out RO address when it is the same as one of the passed addresses" in new Setup {
      val addresses = Seq(addr1, addr2, addr3)
      val resMap = Map(0 -> addr2, 1 -> addr3)
      val roAddress = addr1
      val otherAddressOpt = None
      service.filterAddresses(addresses, roAddress, otherAddressOpt) shouldBe resMap
    }

    "filter out Other address when it is the same as one of the passed addresses" in new Setup {
      val addresses = Seq(addr1, addr2, addr3)
      val resMap = Map(0 -> addr2)
      val roAddress = addr3
      val otherAddressOpt = Some(addr1)
      service.filterAddresses(addresses, roAddress, otherAddressOpt) shouldBe resMap
    }
    "handle an empty list" in new Setup {
      val addresses = Seq.empty
      val resMap = Map.empty
      val roAddress = addr3
      val otherAddressOpt = None
      service.filterAddresses(addresses, roAddress, otherAddressOpt) shouldBe resMap
    }
  }

  "GetAddresses" should {
    "return a list of addresses" in new Setup {
      val regId = "regID"
      when(mockBusinessRegistrationConnector.retrieveAddresses(ArgumentMatchers.contains(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Seq(addr1, addr2)))
      when(mockS4LService.saveMap(ArgumentMatchers.contains("PrePopAddresses"), ArgumentMatchers.any(), ArgumentMatchers.contains(regId))
          (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Int]](), ArgumentMatchers.any[Format[Address]]()))
        .thenReturn(Future.successful(CacheMap("PrePopAddresses", Map.empty)))

      await(service.getPrePopAddresses(regId, addr3, None)) shouldBe Map(0 -> addr1, 1 -> addr2)
    }
  }

  "SaveAddress" should {
    "save an address" in new Setup {
      val regId = "12345"
      when(mockBusinessRegistrationConnector.upsertAddress(ArgumentMatchers.contains(regId), ArgumentMatchers.any[Address]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(addr1))

      await(service.saveAddress(regId, addr1)) shouldBe addr1
    }
  }

  "GetAddress" should {
    "fetch an address by ID" in new Setup {
      val regId = "9999"
      when(mockS4LService.fetchAndGetMap(ArgumentMatchers.contains("PrePopAddresses"), ArgumentMatchers.contains(regId))
          (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Int]](), ArgumentMatchers.any[Format[Address]]()))
        .thenReturn(Future.successful(Some(Map(0 -> addr1, 1 -> addr2, 2 -> addr3))))

      await(service.getAddress(regId, 1)) shouldBe addr2
    }
    "throw an exception when no addresses are returned from S4L" in new Setup {
      val regId = "9999"
      when(mockS4LService.fetchAndGetMap(ArgumentMatchers.contains("PrePopAddresses"), ArgumentMatchers.contains(regId))
      (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Int]](), ArgumentMatchers.any[Format[Address]]()))
        .thenReturn(Future.successful(None))

      intercept[S4LFetchException](await(service.getAddress(regId, 1)))
    }
    "throw an exception when there is no address corresponding to the passed key returned from S4L" in new Setup {
      val regId = "9999"
      when(mockS4LService.fetchAndGetMap(ArgumentMatchers.contains("PrePopAddresses"), ArgumentMatchers.contains(regId))
      (ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[Int]](), ArgumentMatchers.any[Format[Address]]()))
        .thenReturn(Future.successful(Some(Map(0 -> addr1, 1 -> addr2))))

      intercept[S4LFetchException](await(service.getAddress(regId, 2)))
    }
  }
}
