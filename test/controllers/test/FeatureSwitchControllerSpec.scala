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

package controllers.test

import helpers.PayeComponentSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.{BooleanFeatureSwitch, ValueSetFeatureSwitch}

import scala.concurrent.Future

class FeatureSwitchControllerSpec extends PayeComponentSpec {

  val testFeatureSwitch = BooleanFeatureSwitch(name = "companyRegistration", enabled = true)
  val testDisabledSwitch = BooleanFeatureSwitch(name = "companyRegistration", enabled = false)

  class Setup {
    val controller = new FeatureSwitchController(stubMessagesControllerComponents()) {
      override val payeFeatureSwitch = mockFeatureSwitches
      override val featureManager = mockFeatureManager
      override val payeRegConnector = mockPAYERegConnector
    }
  }

  "switcher" should {
    "enable the companyReg feature switch and return an OK" when {
      "companyRegistration and true are passed in the url" in new Setup {
        when(mockFeatureSwitches(ArgumentMatchers.any()))
          .thenReturn(Some(testFeatureSwitch))

        when(mockFeatureManager.enable(ArgumentMatchers.any()))
          .thenReturn(testFeatureSwitch)

        val result = controller.switcher("companyRegistration", "true")(FakeRequest())
        status(result) mustBe OK
      }
    }

    "disable the companyReg feature switch and return an OK" when {
      "companyRegistration and some other featureState is passed into the URL" in new Setup {
        when(mockFeatureSwitches(ArgumentMatchers.any()))
          .thenReturn(Some(testFeatureSwitch))

        when(mockFeatureManager.disable(ArgumentMatchers.any()))
          .thenReturn(testDisabledSwitch)

        val result = controller.switcher("companyRegistration", "someOtherState")(FakeRequest())
        status(result) mustBe OK
      }
    }

    "return a bad request" when {
      "an unknown feature is trying to be enabled" in new Setup {
        when(mockFeatureSwitches(ArgumentMatchers.any()))
          .thenReturn(None)

        val result = controller.switcher("invalidName", "invalidState")(FakeRequest())
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "setting the time" should {
    "update the backend time and return and Ok no matter what happens" in new Setup {
      val date = "2018-12-12T00:00:00"
      val key = "system-date"
      val testFeatureSwitch = ValueSetFeatureSwitch(key, date)

      when(mockFeatureSwitches(ArgumentMatchers.any()))
        .thenReturn(Some(testFeatureSwitch))

      when(mockPAYERegConnector.setBackendDate(ArgumentMatchers.eq(date))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      when(mockFeatureManager.setSystemDate(ArgumentMatchers.any()))
        .thenReturn(testFeatureSwitch)

      val result = controller.switcher(key, date)(FakeRequest())
      status(result) mustBe OK
    }
  }
}
