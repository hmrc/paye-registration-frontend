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

package utils

import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.play.test.UnitSpec

class FeatureSwitchSpec extends UnitSpec with BeforeAndAfterEach {

  override def beforeEach() {
    System.clearProperty("feature.test")
    System.clearProperty("feature.cohoFirstHandOff")
    System.clearProperty("feature.businessActivitiesHandOff")
  }

  "apply" should {

    "return a constructed BooleanFeatureSwitch if the set system property is a boolean" in {
      System.setProperty("feature.test", "true")

      FeatureSwitch("test") shouldBe BooleanFeatureSwitch("test", enabled = true)
    }

    "create an instance of BooleanFeatureSwitch which inherits FeatureSwitch" in {
      FeatureSwitch("test") shouldBe a[FeatureSwitch]
      FeatureSwitch("test") shouldBe a[BooleanFeatureSwitch]
    }
  }

  "unapply" should {

    "deconstruct a given FeatureSwitch into it's name and a false enabled value if undefined as a system property" in {
      val fs = FeatureSwitch("test")

      FeatureSwitch.unapply(fs) shouldBe Some("test" -> false)
    }

    "deconstruct a given FeatureSwitch into its name and true if defined as true as a system property" in {
      System.setProperty("feature.test", "true")
      val fs = FeatureSwitch("test")

      FeatureSwitch.unapply(fs) shouldBe Some("test" -> true)
    }

    "deconstruct a given FeatureSwitch into its name and false if defined as false as a system property" in {
      System.setProperty("feature.test", "false")
      val fs = FeatureSwitch("test")

      FeatureSwitch.unapply(fs) shouldBe Some("test" -> false)
    }
  }

  "getProperty" should {

    "return a disabled feature switch if the system property is undefined" in {
      FeatureSwitch.getProperty("test") shouldBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return an enabled feature switch if the system property is defined as 'true'" in {
      System.setProperty("feature.test", "true")

      FeatureSwitch.getProperty("test") shouldBe BooleanFeatureSwitch("test", enabled = true)
    }

    "return an enabled feature switch if the system property is defined as 'false'" in {
      System.setProperty("feature.test", "false")

      FeatureSwitch.getProperty("test") shouldBe BooleanFeatureSwitch("test", enabled = false)
    }
  }

  "systemPropertyName" should {

    "append feature. to the supplied string'" in {
      FeatureSwitch.systemPropertyName("test") shouldBe "feature.test"
    }
  }

  "setProperty" should {

    "return a feature switch (testKey, false) when supplied with (testKey, testValue)" in {
      FeatureSwitch.setProperty("test", "testValue") shouldBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return a feature switch (testKey, true) when supplied with (testKey, true)" in {
      FeatureSwitch.setProperty("test", "true") shouldBe BooleanFeatureSwitch("test", enabled = true)
    }
  }

  "enable" should {
    "set the value for the supplied key to 'true'" in {
      val fs = FeatureSwitch("test")
      System.setProperty("feature.test", "false")

      FeatureSwitch.enable(fs) shouldBe BooleanFeatureSwitch("test", enabled = true)
    }
  }

  "disable" should {
    "set the value for the supplied key to 'false'" in {
      val fs = FeatureSwitch("test")
      System.setProperty("feature.test", "true")

      FeatureSwitch.disable(fs) shouldBe BooleanFeatureSwitch("test", enabled = false)
    }
  }

  "dynamic toggling should be supported" in {
    val fs = FeatureSwitch("test")

    FeatureSwitch.disable(fs).enabled shouldBe false
    FeatureSwitch.enable(fs).enabled shouldBe true
  }

  "SCRSFeatureSwitches" should {
    "return a disabled feature when the associated system property doesn't exist" in {
      PAYEFeatureSwitches.addressFrontend.enabled shouldBe false
    }

    "return an enabled feature when the associated system property is true" in {
      FeatureSwitch.enable(PAYEFeatureSwitches.addressFrontend)

      PAYEFeatureSwitches.addressFrontend.enabled shouldBe true
    }

    "return a disable feature when the associated system property is false" in {
      FeatureSwitch.disable(PAYEFeatureSwitches.addressFrontend)

      PAYEFeatureSwitches.addressFrontend.enabled shouldBe false
    }

    "return true if the addressService system property is true" in {
      System.setProperty("feature.addressService", "true")

      PAYEFeatureSwitches("addressService") shouldBe Some(BooleanFeatureSwitch("addressService", true))
    }

    "return false if the addressService system property is false" in {
      System.setProperty("feature.addressService", "false")

      PAYEFeatureSwitches("addressService") shouldBe Some(BooleanFeatureSwitch("addressService", false))
    }

    "return an empty option if a system property doesn't exist when using the apply function" in {
      PAYEFeatureSwitches("somethingElse") shouldBe None
    }
  }

}
