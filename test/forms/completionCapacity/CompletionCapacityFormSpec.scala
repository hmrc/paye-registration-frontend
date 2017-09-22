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

package forms.completionCapacity

import enums.UserCapacity
import models.view.CompletionCapacity
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class CompletionCapacityFormSpec extends UnitSpec {

  val testForm = CompletionCapacityForm.form

  "Binding CompletionCapacity Form to a model" should {
    "Bind successfully when capacity is Director" in {
      val data = Map(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> "")

      val model = CompletionCapacity(UserCapacity.director, "")

      val boundModel = testForm.bind(data).fold(
        errs => errs,
        success => success
      )

      boundModel shouldBe model
    }

    "Bind successfully when capacity is Secretary" in {
      val data = Map(
        "completionCapacity" -> "secretary",
        "completionCapacityOther" -> "")

      val model = CompletionCapacity(UserCapacity.secretary, "")

      val boundModel = testForm.bind(data).fold(
        errs => errs,
        success => success
      )

      boundModel shouldBe model
    }

    "Bind successfully when capacity is Agent" in {
      val data = Map(
        "completionCapacity" -> "agent",
        "completionCapacityOther" -> "")

      val model = CompletionCapacity(UserCapacity.agent, "")

      val boundModel = testForm.bind(data).fold(
        errs => errs,
        success => success
      )

      boundModel shouldBe model
    }

    "Bind successfully when capacity is Other" in {
      val data = Map(
        "completionCapacity" -> "other",
        "completionCapacityOther" -> "unimportant")

      val model = CompletionCapacity(UserCapacity.other, "unimportant")

      val boundModel = testForm.bind(data).fold(
        errs => errs,
        success => success
      )

      boundModel shouldBe model
    }

    "Fail to bind when capacity is incomplete" in {
      val data = Map(
        "completionCapacity" -> "",
        "completionCapacityOther" -> "unimportant")

      val boundForm = testForm.bind(data)

      boundForm.errors shouldBe Seq(FormError("completionCapacity", "pages.completionCapacity.error"))
    }

    "Fail to bind when other capacity is incomplete" in {
      val data = Map(
        "completionCapacity" -> "other",
        "completionCapacityOther" -> "")

      val boundForm = testForm.bind(data)

      boundForm.errors shouldBe Seq(FormError("completionCapacityOther", "pages.completionCapacity.other.error"))
    }
  }

  "Unbinding a Completion Capacity model to a form" should {
    "Unbind successfully when capacity is Director" in {
      val data = Map(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> "")

      val model = CompletionCapacity(UserCapacity.director, "")

      val filledForm = testForm.fill(model)

      filledForm.data shouldBe data
    }
  }
}
