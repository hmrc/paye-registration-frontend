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

package forms.test

import models.test.CoHoCompanyDetailsFormModel
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class TestCoHoCompanyDetailsFormSpec extends UnitSpec {
  val testForm = TestCoHoCompanyDetailsForm.form

  "Binding TestCoHoCompanyDetailsForm to a model" when {
    "Bind successfully with full data" should {
      val data = Map(
        "companyName" -> "TEST LTD",
        "sicCodes[0]" -> "166",
        "sicCodes[1]" -> "84",
        "sicCodes[2]" -> "",
        "descriptions[0]" -> "consulting",
        "descriptions[1]" -> "laundring"
      )

      val model = CoHoCompanyDetailsFormModel(
        companyName = "TEST LTD",
        sicCodes = List("166", "84", ""),
        descriptions = List("consulting", "laundring")
      )

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel shouldBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data shouldBe Map(
          "companyName" -> "TEST LTD",
          "sicCodes[0]" -> "166",
          "sicCodes[1]" -> "84",
          "sicCodes[2]" -> "",
          "descriptions[0]" -> "consulting",
          "descriptions[1]" -> "laundring"
        )
      }
    }
  }

  "Have the correct error" when {
    "company name is not completed" in {
      val data: Map[String, String] = Map(
        "companyName" -> "",
        "sicCodes[0]" -> "",
        "descriptions[0]" -> ""
      )
      val boundForm = testForm.bind(data)
      val nameError = FormError("companyName", "error.required")

      boundForm.errors shouldBe Seq(nameError)
      boundForm.data shouldBe data
    }
  }
}
