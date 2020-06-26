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

package utils

import helpers.PayeComponentSpec

class FeatureSwitchSpec extends PayeComponentSpec {

  override def beforeEach() {
    System.clearProperty("feature.test")
    System.clearProperty("feature.cohoFirstHandOff")
    System.clearProperty("feature.businessActivitiesHandOff")
    System.clearProperty("feature.system-date")
    super.beforeEach()
  }

  val featureSwitch = new FeatureSwitchManager
  val payeFeatureSwitch = new PAYEFeatureSwitch(featureSwitch)
  val booleanFeatureSwitch = BooleanFeatureSwitch("test", false)

  "apply" should {

    "return a constructed BooleanFeatureSwitch if the set system property is a boolean" in {
      System.setProperty("feature.test", "true")

      featureSwitch("test") mustBe BooleanFeatureSwitch("test", enabled = true)
    }

    "create an instance of BooleanFeatureSwitch which inherits FeatureSwitch" in {
      featureSwitch("test") mustBe a[FeatureSwitch]
      featureSwitch("test") mustBe a[BooleanFeatureSwitch]
    }

    "create an instance of EnabledTimedFeatureSwitch which inherits FeatureSwitch" in {
      System.setProperty("feature.test", "2016-05-05T14:30:00_2016-05-08T14:30:00")

      featureSwitch("test") mustBe a[FeatureSwitch]
      featureSwitch("test") mustBe a[TimedFeatureSwitch]
      featureSwitch("test") mustBe a[EnabledTimedFeatureSwitch]
    }

    "return an enabled EnabledTimedFeatureSwitch when only the end datetime is specified and is in the future" in {
      System.setProperty("feature.test", "X_9999-05-08T14:30:00")

      featureSwitch("test") mustBe a[EnabledTimedFeatureSwitch]
      featureSwitch("test").enabled mustBe true
    }

    "return a disabled EnabledTimedFeatureSwitch when only the end datetime is specified and is in the past" in {
      System.setProperty("feature.test", "X_2000-05-08T14:30:00")

      featureSwitch("test") mustBe a[EnabledTimedFeatureSwitch]
      featureSwitch("test").enabled mustBe false
    }

    "return an enabled EnabledTimedFeatureSwitch when only the start datetime is specified and is in the past" in {
      System.setProperty("feature.test", "2000-05-05T14:30:00_X")

      featureSwitch("test") mustBe a[EnabledTimedFeatureSwitch]
      featureSwitch("test").enabled mustBe true
    }

    "return a disabled TimedFeatureSwitch when neither date is specified" in {
      System.setProperty("feature.test", "X_X")

      featureSwitch("test").enabled mustBe false
    }

    "create an instance of DisabledTimedFeatureSwitch which inherits FeatureSwitch" in {
      System.setProperty("feature.test", "!2016-05-05T14:30:00_2016-05-08T14:30:00")

      featureSwitch("test") mustBe a[FeatureSwitch]
      featureSwitch("test") mustBe a[TimedFeatureSwitch]
      featureSwitch("test") mustBe a[DisabledTimedFeatureSwitch]
    }

    "return an enabled DisabledTimedFeatureSwitch when only the end datetime is specified and is in the future" in {
      System.setProperty("feature.test", "!X_9999-05-08T14:30:00")

      featureSwitch("test") mustBe a[DisabledTimedFeatureSwitch]
      featureSwitch("test").enabled mustBe false
    }

    "return a disabled DisabledTimedFeatureSwitch when only the end datetime is specified and is in the past" in {
      System.setProperty("feature.test", "!X_2000-05-08T14:30:00")

      featureSwitch("test") mustBe a[DisabledTimedFeatureSwitch]
      featureSwitch("test").enabled mustBe true
    }

    "return an enabled DisabledTimedFeatureSwitch when only the start datetime is specified and is in the past" in {
      System.setProperty("feature.test", "!2000-05-05T14:30:00_X")

      featureSwitch("test") mustBe a[DisabledTimedFeatureSwitch]
      featureSwitch("test").enabled mustBe false
    }

    "return an enabled DisabledTimedFeatureSwitch when neither date is specified" in {
      System.setProperty("feature.test", "!X_X")

      featureSwitch("test").enabled mustBe true
    }
  }

  "unapply" should {

    "deconstruct a given FeatureSwitch into it's name and a false enabled value if undefined as a system property" in {
      val fs = featureSwitch("test")

      featureSwitch.unapply(fs) mustBe Some("test" -> false)
    }

    "deconstruct a given FeatureSwitch into its name and true if defined as true as a system property" in {
      System.setProperty("feature.test", "true")
      val fs = featureSwitch("test")

      featureSwitch.unapply(fs) mustBe Some("test" -> true)
    }

    "deconstruct a given FeatureSwitch into its name and false if defined as false as a system property" in {
      System.setProperty("feature.test", "false")
      val fs = featureSwitch("test")

      featureSwitch.unapply(fs) mustBe Some("test" -> false)
    }

    "deconstruct a given TimedFeatureSwitch into its name and enabled flag if defined as a system property" in {
      System.setProperty("feature.test", "2016-05-05T14:30:00_2016-05-08T14:30:00")
      val fs = featureSwitch("test")

      featureSwitch.unapply(fs) mustBe Some("test" -> false)
    }
  }

  "getProperty" should {
    "return a disabled feature switch if the system property is undefined" in {
      featureSwitch.getProperty("test") mustBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return an enabled feature switch if the system property is defined as 'true'" in {
      System.setProperty("feature.test", "true")

      featureSwitch.getProperty("test") mustBe BooleanFeatureSwitch("test", enabled = true)
    }

    "return an enabled feature switch if the system property is defined as 'false'" in {
      System.setProperty("feature.test", "false")

      featureSwitch.getProperty("test") mustBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return a EnabledTimedFeatureSwitch when the set system property is a date" in {
      System.setProperty("feature.test", "2016-05-05T14:30:00_2016-05-08T14:30:00")

      featureSwitch.getProperty("test") mustBe a[EnabledTimedFeatureSwitch]
    }

    "return a DisabledTimedFeatureSwitch when the set system property is a date" in {
      System.setProperty("feature.test", "!2016-05-05T14:30:00_2016-05-08T14:30:00")

      featureSwitch.getProperty("test") mustBe a[DisabledTimedFeatureSwitch]
    }
  }

  "systemPropertyName" should {
    "append feature. to the supplied string'" in {
      featureSwitch.systemPropertyName("test") mustBe "feature.test"
    }
  }

  "setProperty" should {

    "return a feature switch (testKey, false) when supplied with (testKey, testValue)" in {
      featureSwitch.setProperty("test", "testValue") mustBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return a feature switch (testKey, true) when supplied with (testKey, true)" in {
      featureSwitch.setProperty("test", "true") mustBe BooleanFeatureSwitch("test", enabled = true)
    }

    "return ValueSetFeatureSwitch when supplied system-date and 2018-01-01" in {
      featureSwitch.setProperty("system-date", "2018-01-01T12:00:00") mustBe ValueSetFeatureSwitch("system-date", "2018-01-01T12:00:00")
    }
  }

  "enable" should {
    "set the value for the supplied key to 'true'" in {
      System.setProperty("feature.test", "false")

      featureSwitch.enable(booleanFeatureSwitch) mustBe BooleanFeatureSwitch("test", enabled = true)
    }
  }

  "disable" should {
    "set the value for the supplied key to 'false'" in {
      System.setProperty("feature.test", "true")

      featureSwitch.disable(booleanFeatureSwitch) mustBe BooleanFeatureSwitch("test", enabled = false)
    }
  }

  "dynamic toggling should be supported" in {
    featureSwitch.disable(booleanFeatureSwitch).enabled mustBe false
    featureSwitch.enable(booleanFeatureSwitch).enabled mustBe true
  }

  "SCRSFeatureSwitches" should {
    "return a disabled feature when the associated system property doesn't exist" in {
      payeFeatureSwitch.companyReg.enabled mustBe false
    }

    "return an enabled feature when the associated system property is true" in {
      featureSwitch.enable(payeFeatureSwitch.companyReg)

      payeFeatureSwitch.companyReg.enabled mustBe true
    }

    "return a disable feature when the associated system property is false" in {
      featureSwitch.disable(payeFeatureSwitch.companyReg)

      payeFeatureSwitch.companyReg.enabled mustBe false
    }

    "return true if the companyRegistration system property is true" in {
      System.setProperty("feature.companyRegistration", "true")

      payeFeatureSwitch("companyRegistration") mustBe Some(BooleanFeatureSwitch("companyRegistration", true))
    }

    "return false if the companyRegistration system property is false" in {
      System.setProperty("feature.companyRegistration", "false")

      payeFeatureSwitch("companyRegistration") mustBe Some(BooleanFeatureSwitch("companyRegistration", false))
    }

    "return an empty option if a system property doesn't exist when using the apply function" in {
      payeFeatureSwitch("somethingElse") mustBe None
    }
  }
}
