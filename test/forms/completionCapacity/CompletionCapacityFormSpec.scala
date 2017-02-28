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

import models.view.CompletionCapacity
import uk.gov.hmrc.play.test.UnitSpec

class CompletionCapacityFormSpec extends UnitSpec {

  val testForm = CompletionCapacityForm.form

  "Binding CompletionCapacity Form to a model" should {
    "Bind successfully when capacity is Director" in {
      val data = Map(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> "")

      val model = CompletionCapacity("director", "")

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

      val model = CompletionCapacity("agent", "")

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

      val model = CompletionCapacity("other", "unimportant")

      val boundModel = testForm.bind(data).fold(
        errs => errs,
        success => success
      )

      boundModel shouldBe model
    }

  }

  "Unbinding a Completion Capacity model to a form" should {

    "Unbind successfully when capacity is Director" in {
      val data = Map(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> "")

      val model = CompletionCapacity("director", "")

      val filledForm = testForm.fill(model)

      filledForm.data shouldBe data
    }
  }

}
