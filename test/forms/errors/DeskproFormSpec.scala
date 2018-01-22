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

package forms.errors

import uk.gov.hmrc.play.test.UnitSpec

class DeskproFormSpec extends UnitSpec {

  class Setup {
    val REQUIRED = "error.required"
    val ERR_PREFIX = "errorPages.failedSubmission.error."
    val empty: Map[String, String] = Map()
    val simpleData = Map(
      "name" -> "foo",
      "email" -> "foo@bar.com",
      "message" -> "foo bar"
    )
    val testForm = DeskproForm.form
  }

  "Creating a form with a valid post" should {
    "have no errors" in new Setup {
      testForm.bind(simpleData).hasErrors shouldBe false
    }
  }

  "Creating a form with an empty post" should {
    "have mandatory errors for all fields when no submitted data" in new Setup {
      val form = testForm.bind(empty)
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("name", REQUIRED),
        ("email", REQUIRED),
        ("message", REQUIRED)
      )
    }
  }

  "Creating a form where name" should {
    "have mandatory errors if missing" in new Setup {
      val form = testForm.bind(simpleData - "name")
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("name", REQUIRED)
      )
    }

    "be ok when at the length limit" in new Setup {
      val data = simpleData ++ Map("name" -> "x"*70)
      val form = testForm.bind(data)
      form.hasErrors shouldBe false
    }

    "have a size error when too long" in new Setup {
      val data = simpleData ++ Map("name" -> "x"*71)
      val form = testForm.bind(data)
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("name", ERR_PREFIX + "name_too_long")
      )
    }

    "have a character error when it has dodgy characters" in new Setup {
      val data = simpleData ++ Map("name" -> "<>%$")
      val form = testForm.bind(data)
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("name", ERR_PREFIX + "name_invalid_characters")
      )
    }
  }

  "Creating a form where email" should {
    "have mandatory errors if missing" in new Setup {
      val form = testForm.bind(simpleData - "email")
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("email", REQUIRED)
      )
    }
    "be ok when at the length limit" in new Setup {
      val part = "x"*49 + "."
      val email = "x"*51 + "@" + part + part + part + part + "com"

      email.length shouldBe 255

      val data = simpleData ++ Map("email" -> email)
      val form = testForm.bind(data)
      form.hasErrors shouldBe false
    }

    "have a size error when too long" in new Setup {
      val part = "x"*49 + "."
      val email = "x"*52 + "@" + part + part + part + part + "com"

      email.length shouldBe 256

      val data = simpleData ++ Map("email" -> email)
      val form = testForm.bind(data)
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("email", ERR_PREFIX + "email_too_long")
      )
    }

    Map(
      ".." -> "x@x..com",
      "user too long" -> ("x"*65 + "@x.com"),
      "domain part too long" -> ("xxx@" + "x"*65 + ".com")
    ) foreach {
      case ((m, email)) =>
        s"have a format error with ${m}" in new Setup {
          val data = simpleData ++ Map("email" -> email)
          val form = testForm.bind(data)
          form.hasErrors shouldBe true
          form.errors map { e =>
            (e.key, e.message)
          } shouldBe Seq(
            ("email", ERR_PREFIX + "email_format")
          )
        }
    }
  }

  "Creating a form where messages" should {
    "have mandatory errors if missing" in new Setup {
      val form = testForm.bind(simpleData - "message")
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("message", REQUIRED)
      )
    }

    "be ok when at the length limit" in new Setup {
      val data = simpleData ++ Map("message" -> "x"*1000)
      val form = testForm.bind(data)
      form.hasErrors shouldBe false
    }

    "have a size error when too long" in new Setup {
      val data = simpleData ++ Map("message" -> "x"*1001)
      val form = testForm.bind(data)
      form.hasErrors shouldBe true
      form.errors map { e =>
        (e.key, e.message)
      } shouldBe Seq(
        ("message", ERR_PREFIX + "message_too_long")
      )
    }
  }

}