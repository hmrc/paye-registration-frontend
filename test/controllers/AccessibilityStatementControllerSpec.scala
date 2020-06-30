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

package controllers

import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class AccessibilityStatementControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val testAccessibilityStatementController = new AccessibilityStatementController(stubMessagesControllerComponents())(mockAppConfig)

  "show" should {
    "return OK" in {

      val result = testAccessibilityStatementController.show("test")(FakeRequest("GET", "/"))

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
  }
}
