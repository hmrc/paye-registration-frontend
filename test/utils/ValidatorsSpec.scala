/*
 * Copyright 2018 HM Revenue & Customs
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

class ValidatorsSpec extends PayeComponentSpec with DateUtil {

  "dateRegex" should {
    "validate 2018-01-01T12:00:00" in {
      assert("2018-01-01T12:00:00".matches(Validators.datePatternRegex))
    }

    "not validate 2018-1-1" in {
      assert(!"2018-1-1".matches(Validators.datePatternRegex))
    }
  }

  "desSchemaRegex" should {
    "not validate FAKE_SOD::TR9873!^^7FDFNN" in {
      assert(!"FAKE_SOD::TR9873!^^7FDFNN".matches(Validators.desSessionRegex))
    }
    "validate stubbed-1sds-sdijhi-2383-seei" in {
      assert("stubbed-1sds-sdijhi-2383-seei".matches(Validators.desSessionRegex))
    }
  }
}