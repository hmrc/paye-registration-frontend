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

package forms.errors

import models.view.Ticket
import testHelpers.PAYERegSpec

class DeskproFormSpec extends PAYERegSpec{

  val name = "Valid Name"
  val email = "abc@cba.abc"
  val message = "I am a test message"


  val validData = Map(
    "name" -> name,
    "email" -> email,
    "message" -> message
  )

  val ticket = Ticket(name, email, message)


  "Deskpro form" should {
    "bind correctly from the data" in {
      val result = DeskproForm.form.bind(validData).fold(
        errors => errors,
        success => success
      )

      result shouldBe ticket
    }
    "unapply the model correctly" in {
      val result = DeskproForm.form.fill(ticket)

      result.data shouldBe validData
    }
  }


}
