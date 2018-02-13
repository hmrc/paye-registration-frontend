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

package enums

import helpers.PayeComponentSpec

class UserCapacitySpec extends PayeComponentSpec {

  "Reading UserCapacity from string" should {
    "work for director" in {
      UserCapacity.fromString("director") mustBe UserCapacity.director
    }
    "work for DiReCToR" in {
      UserCapacity.fromString("DiReCToR") mustBe UserCapacity.director
    }
    "work for agent" in {
      UserCapacity.fromString("agent") mustBe UserCapacity.agent
    }
    "work for secretary" in {
      UserCapacity.fromString("company secretary") mustBe UserCapacity.secretary
    }
    "work for OTHER" in {
      UserCapacity.fromString("OTHER") mustBe UserCapacity.other
    }
  }

  "Writing UserCapacity to string" should {
    "work for director" in {
      UserCapacity.director.toString mustBe "director"
    }
    "work for agent" in {
      UserCapacity.agent.toString mustBe "agent"
    }
    "work for secretary" in {
      UserCapacity.secretary.toString mustBe "company secretary"
    }
    "work for other" in {
      UserCapacity.other.toString mustBe "other"
    }
  }

}
