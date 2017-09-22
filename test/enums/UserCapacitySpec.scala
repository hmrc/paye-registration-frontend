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

package enums

import uk.gov.hmrc.play.test.UnitSpec

class UserCapacitySpec extends UnitSpec {

  "Reading UserCapacity from string" should {
    "work for director" in {
      UserCapacity.fromString("director") shouldBe UserCapacity.director
    }
    "work for DiReCToR" in {
      UserCapacity.fromString("DiReCToR") shouldBe UserCapacity.director
    }
    "work for agent" in {
      UserCapacity.fromString("agent") shouldBe UserCapacity.agent
    }
    "work for secretary" in {
      UserCapacity.fromString("company secretary") shouldBe UserCapacity.secretary
    }
    "work for OTHER" in {
      UserCapacity.fromString("OTHER") shouldBe UserCapacity.other
    }
  }

  "Writing UserCapacity to string" should {
    "work for director" in {
      UserCapacity.director.toString shouldBe "director"
    }
    "work for agent" in {
      UserCapacity.agent.toString shouldBe "agent"
    }
    "work for secretary" in {
      UserCapacity.secretary.toString shouldBe "company secretary"
    }
    "work for other" in {
      UserCapacity.other.toString shouldBe "other"
    }
  }

}
