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

package forms.helpers

import java.time.LocalDate

import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class DateFormSpec extends UnitSpec {

  object TestForm extends DateForm {

    override val prefix = "tst"
    override def validation(dt: LocalDate) = Right(dt)

  }


  "A form extending DateForm" should {
    "bind from a valid date" in {
      val data = Map[String, String](
        "tstDay" -> "28",
        "tstMonth" -> "01",
        "tstYear" -> "2016"
        )
      val date = LocalDate.of(2016,1,28)
      TestForm.dateFormatter.bind("tstKey", data) shouldBe Right(date)
    }
  }

  "fail to bind from an invalid date (missing day)" in {
    val data = Map[String, String](
        "tstMonth" -> "01",
        "tstYear" -> "2016"
        )
    TestForm.dateFormatter.bind("tstKey", data) shouldBe Left(Seq(FormError("tstDay", "app.common.date.invalid")))
  }

  "fail to bind from an invalid date (missing month)" in {
    val data = Map[String, String](
        "tstDay" -> "28",
        "tstYear" -> "2016"
        )
    TestForm.dateFormatter.bind("tstKey", data) shouldBe Left(Seq(FormError("tstDay", "app.common.date.invalid")))
  }

  "fail to bind from an invalid date (missing year)" in {
    val data = Map[String, String](
        "tstDay" -> "28",
        "tstMonth" -> "01"
        )
    TestForm.dateFormatter.bind("tstKey", data) shouldBe Left(Seq(FormError("tstDay", "app.common.date.invalid")))
  }

  "unbind from a LocalDate" in {
    val date = LocalDate.of(2016,2,29)
    val data = Map[String, String](
        "tstDay" -> "29",
        "tstMonth" -> "2",
        "tstYear" -> "2016"
        )
    TestForm.dateFormatter.unbind("testKey", date) shouldBe data
  }

}
