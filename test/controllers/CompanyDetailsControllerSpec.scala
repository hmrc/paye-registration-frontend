/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import connectors.S4LConnector
import helpers.PAYERegSpec
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

class CompanyDetailsControllerSpec extends PAYERegSpec {

  val mockS4LConnector = mock[S4LConnector]

  object TestCompanyDetailsController extends CompanyDetailsController {
    override val s4LConnector = mockS4LConnector
  }

  val fakeRequest = FakeRequest("GET", "/")


  "GET /trading-name" should {
    "return 200" in {
      val result = CompanyDetailsController.tradingName(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = CompanyDetailsController.tradingName(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }


  }

}
