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

package enums

import common.exceptions.InternalExceptions._
import helpers.PayeComponentSpec

class YesNoSpec extends PayeComponentSpec {

  "YesNo enum" should {
    "correctly handle 'yes'" in {
      YesNo.fromString("yes") mustBe YesNo.Yes
    }
    "correctly handle 'YeS" in {
      YesNo.fromString("YeS") mustBe YesNo.Yes
    }
    "correctly handle 'no'" in {
      YesNo.fromString("no") mustBe YesNo.No
    }
    "correctly handle 'nO'" in {
      YesNo.fromString("nO") mustBe YesNo.No
    }
    "throw the correct exception" in {
      a[UnableToCreateEnumException] should be thrownBy YesNo.fromString("wrongInput")
    }
    "correctly handle true" in {
      YesNo.fromBoolean(true) mustBe YesNo.Yes
    }
    "correctly handle false" in {
      YesNo.fromBoolean(false) mustBe YesNo.No
    }
  }

}
