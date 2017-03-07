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

package controllers.test

import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK}
import testHelpers.PAYERegSpec
import utils.{BooleanFeatureSwitch, FeatureManager, PAYEFeatureSwitches}

import scala.concurrent.Future

class FeatureSwitchControllerSpec extends PAYERegSpec {

  val mockPAYEFeatureSwitch = mock[PAYEFeatureSwitches]
  val mockFeatureManager = mock[FeatureManager]

  val testFeatureSwitch = BooleanFeatureSwitch(name = "addressService", enabled = true)
  val testDisabledSwitch = BooleanFeatureSwitch(name = "addressService", enabled = false)

  class Setup {
    val controller = new FeatureSwitchCtrl {
      override val PayeFeatureSwitch = mockPAYEFeatureSwitch
      override val featureManager = mockFeatureManager
    }
  }

  "addressServiceSwitch" should {
    "enable the addressService feature switch and return an OK" when {
      "addressService and addressLookUpFrontend are passed in the url" in new Setup {
        when(mockPAYEFeatureSwitch(Matchers.any()))
          .thenReturn(Future.successful(Some(testFeatureSwitch)))

        when(mockFeatureManager.enable(Matchers.any()))
          .thenReturn(testFeatureSwitch)

        val result = controller.addressServiceSwitch("addressService","addressLookupFrontend")(FakeRequest())
        status(result) shouldBe OK
      }
    }

    "disable the addressServiceFeature switch and return an OK" when {
      "addressService and some other featureState is passed into the URL" in new Setup {
        when(mockPAYEFeatureSwitch(Matchers.any()))
          .thenReturn(Future.successful(Some(testFeatureSwitch)))

        when(mockFeatureManager.disable(Matchers.any()))
          .thenReturn(testDisabledSwitch)

        val result = await(controller.addressServiceSwitch("addressService","someOtherState")(FakeRequest()))
        status(result) shouldBe OK
      }
    }

    "return a bad request" when {
      "an unknown feature is trying to be enabled" in new Setup {
        when(mockPAYEFeatureSwitch(Matchers.any()))
          .thenReturn(Future.successful(None))

        val result = controller.addressServiceSwitch("invalidName","invalidState")(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
