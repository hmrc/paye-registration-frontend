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

import builders.AuthBuilder
import connectors._
import enums.{AccountTypes, DownstreamOutcome}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

class PAYERegistrationServiceSpec extends PAYERegSpec {

  val mockRegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new PAYERegistrationSrv {
      override val authConnector = mockAuthConnector
      override val payeRegistrationConnector = mockRegConnector
    }
  }

  implicit val hc = HeaderCarrier()
  implicit val context = AuthBuilder.createTestUser


  val forbidden = Upstream4xxResponse("403", 403, 403)
  val notFound = new NotFoundException("404")
  val runTimeException = new RuntimeException("tst")

  "Calling assertRegistrationFootprint" should {
    "return a success response when the Registration is successfully created" in new Setup {
      when(mockRegConnector.createNewRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      await(service.assertRegistrationFootprint("123", "txID")) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when the Registration can't be created" in new Setup {
      when(mockRegConnector.createNewRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      await(service.assertRegistrationFootprint("123", "txID")) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getAccountAffinityGroup" should {
    "return a AccountType.Organisation" when {
      "the authenticated user has an org account" in new Setup {
        val userDetails =
          Json.obj(
            "affinityGroup" -> "Organisation"
          )

        when(mockAuthConnector.getUserDetails[JsObject](ArgumentMatchers.any[AuthContext]())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(userDetails))

        val result = await(service.getAccountAffinityGroup)
        result shouldBe AccountTypes.Organisation
      }
    }

    "return an AccountType.InvalidAccountType" when {
      "the authenticated user doesn't have an org account" in new Setup {
        val userDetails =
          Json.obj(
            "affinityGroup" -> "Individual"
          )

        when(mockAuthConnector.getUserDetails[JsObject](ArgumentMatchers.any[AuthContext]())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(userDetails))

        val result = await(service.getAccountAffinityGroup)
        result shouldBe AccountTypes.InvalidAccountType
      }
    }
  }
}
