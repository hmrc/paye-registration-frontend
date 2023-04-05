/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import utils.{BooleanFeatureSwitch, ValueSetFeatureSwitch}

import scala.concurrent.Future

class FeatureSwitchControllerSpec extends PayeComponentSpec with GuiceOneAppPerSuite {

  val testFeatureSwitch: BooleanFeatureSwitch = BooleanFeatureSwitch(name = "companyRegistration", enabled = true)
  val testDisabledSwitch: BooleanFeatureSwitch = BooleanFeatureSwitch(name = "companyRegistration", enabled = false)
  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  class Setup {
    val controller: FeatureSwitchController = new FeatureSwitchController(
      featureManager = mockFeatureManager,
      payeFeatureSwitch = mockFeatureSwitch,
      payeRegConnector = mockPAYERegConnector,
      mcc = mockMcc
    )
  }

  "switcher" should {
    "enable the companyReg feature switch and return an OK" when {
      "companyRegistration and true are passed in the url" in new Setup {
        when(mockFeatureSwitch(ArgumentMatchers.any()))
          .thenReturn(Some(testFeatureSwitch))

        when(mockFeatureManager.enable(ArgumentMatchers.any()))
          .thenReturn(testFeatureSwitch)

        val result: Future[Result] = controller.switcher("companyRegistration", "true")(FakeRequest())
        status(result) mustBe OK
      }
    }

    "disable the companyReg feature switch and return an OK" when {
      "companyRegistration and some other featureState is passed into the URL" in new Setup {
        when(mockFeatureSwitch(ArgumentMatchers.any()))
          .thenReturn(Some(testFeatureSwitch))

        when(mockFeatureManager.disable(ArgumentMatchers.any()))
          .thenReturn(testDisabledSwitch)

        val result: Future[Result] = controller.switcher("companyRegistration", "someOtherState")(FakeRequest())
        status(result) mustBe OK
      }
    }

    "return a bad request" when {
      "an unknown feature is trying to be enabled" in new Setup {
        when(mockFeatureSwitch(ArgumentMatchers.any()))
          .thenReturn(None)

        val result: Future[Result] = controller.switcher("invalidName", "invalidState")(FakeRequest())
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "setting the time" should {
    "update the backend time and return and Ok no matter what happens" in new Setup {
      val date = "2018-12-12T00:00:00"
      val key = "system-date"
      val testFeatureSwitch: ValueSetFeatureSwitch = ValueSetFeatureSwitch(key, date)

      when(mockFeatureSwitch(ArgumentMatchers.any()))
        .thenReturn(Some(testFeatureSwitch))

      when(mockPAYERegConnector.setBackendDate(ArgumentMatchers.eq(date))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      when(mockFeatureManager.setSystemDate(ArgumentMatchers.any()))
        .thenReturn(testFeatureSwitch)

      val result: Future[Result] = controller.switcher(key, date)(FakeRequest())
      status(result) mustBe OK
    }
  }
}
