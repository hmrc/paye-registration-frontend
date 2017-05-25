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

package forms.directorDetails

import models.view.{Ninos, UserEnteredNino}
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class DirectorDetailsFormSpec extends UnitSpec {

  val testForm = DirectorDetailsForm.form

  "Binding BusinessContactDetailsForm to a model" when {

    "Supplied with full data" should {
      val data = Map(
        "nino[0]" -> "ZY123456A",
        "nino[1]" -> "ZY223456A"
      )
      val model = Ninos(
        ninoMapping = List(
          UserEnteredNino("1", Some("ZY223456A")),
          UserEnteredNino("0", Some("ZY123456A"))
        )
      )
      val orderedModel = Ninos(model.ninoMapping.sortBy(_.id.toInt))

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel shouldBe orderedModel
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data shouldBe Map(
          "nino[0]" -> "ZY 12 34 56 A",
          "nino[1]" -> "ZY 22 34 56 A"
        )
      }
    }

    "Supplied with partial data" should {
      val data = Map(
        "nino[0]" -> "ZY 12 34 56 A",
        "nino[1]" -> "",
        "nino[2]" -> "ZY323456A"
      )
      val model = Ninos(
        ninoMapping = List(
          UserEnteredNino("0", Some("ZY123456A")),
          UserEnteredNino("1", None),
          UserEnteredNino("2", Some("ZY323456A"))
        )
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
          "nino[0]" -> "ZY 12 34 56 A",
          "nino[1]" -> "",
          "nino[2]" -> "ZY 32 34 56 A"
        )
      }
    }

    "Supplied with no data" should {
      val data = Map(
        "nino[0]" -> "",
        "nino[1]" -> "",
        "nino[2]" -> ""
      )

      "Fail to bind with the correct errors" in {
        val boundForm = testForm.bind(data).fold(
          errors => errors,
          success => testForm.fill(success)
        )
        boundForm.errors shouldBe Seq(FormError("noFieldsCompleted-nino[0]", "pages.directorDetails.errors.noneCompleted"))
        boundForm.data shouldBe data
      }
    }

    "Supplied with no data and a csrf token" should {
      val data = Map(
        "csrfToken" -> "CSRFCSRF",
        "nino[0]" -> "",
        "nino[1]" -> "",
        "nino[2]" -> ""
      )

      "Fail to bind with the correct errors" in {
        val boundForm = testForm.bind(data).fold(
          errors => errors,
          success => testForm.fill(success)
        )
        boundForm.errors shouldBe Seq(FormError("noFieldsCompleted-nino[0]", "pages.directorDetails.errors.noneCompleted"))
        boundForm.data shouldBe data
      }
    }

    "Supplied with incorrectly formatted data" should {
      val data = Map(
        "nino[0]" -> "A",
        "nino[1]" -> "AA123456A",
        "nino[2]" -> "BI876392B",
        "nino[3]" -> "AA123456E",
        "nino[4]" -> "AA123456AD",
        "nino[5]" -> "SR  12 11 34 C",
        "nino[6]" -> "ZZ 12 11 34 C",
        "nino[7]" -> "SR 15 33 66 Q",
        "nino[8]" -> "SR 15 33 66 QW",
        "nino[9]" -> "DR 15 33 66 C",
        "nino[10]" -> "SD 15 33 66 C",
        "nino[11]" -> "S  R 15 33 66 C",
        "nino[12]" -> "bi876392b",
        "nino[13]" -> "aa123456e",
        "nino[14]" -> "zz 12 11 34 c",
        "nino[15]" -> "sr 12 11 34 c",
        "nino[16]" -> "     sr 12 11 34 c     "
      )

      val errs = Seq(
        FormError("nino[0]", "errors.invalid.nino"),
        FormError("nino[2]", "errors.invalid.nino"),
        FormError("nino[3]", "errors.invalid.nino"),
        FormError("nino[4]", "errors.invalid.nino"),
        FormError("nino[5]", "errors.invalid.nino"),
        FormError("nino[6]", "errors.invalid.nino"),
        FormError("nino[7]", "errors.invalid.nino"),
        FormError("nino[8]", "errors.invalid.nino"),
        FormError("nino[9]", "errors.invalid.nino"),
        FormError("nino[10]", "errors.invalid.nino"),
        FormError("nino[11]", "errors.invalid.nino"),
        FormError("nino[12]", "errors.invalid.nino"),
        FormError("nino[13]", "errors.invalid.nino"),
        FormError("nino[14]", "errors.invalid.nino")
      )

      "Fail to bind with the correct errors" in {
        val boundForm = testForm.bind(data).fold(
          errors => errors,
          success => testForm.fill(success)
        )
        boundForm.errors shouldBe errs
        boundForm.data shouldBe data
      }
    }
  }

}
