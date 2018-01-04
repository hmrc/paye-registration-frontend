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

package models

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsResult, JsSuccess}
import uk.gov.hmrc.play.test.UnitSpec

trait JsonFormValidation {
  this: UnitSpec =>

  def shouldBeSuccess[T](expected: T, result: JsResult[T]) = {
    result match {
      case JsSuccess(value, path) => value shouldBe expected
      case JsError(errors) => fail(s"Test produced errors - ${errors}")
    }
  }

  def shouldHaveErrors[T](result: JsResult[T], errorPath: JsPath, expectedError: ValidationError): Unit = {
    shouldHaveErrors[T](result, Map(errorPath -> Seq(expectedError)))
  }

  def shouldHaveErrors[T](result: JsResult[T], errorPath: JsPath, expectedErrors: Seq[ValidationError]): Unit = {
    shouldHaveErrors[T](result, Map(errorPath -> expectedErrors))
  }

  def shouldHaveErrors[T](result: JsResult[T], expectedErrors: Map[JsPath, Seq[ValidationError]]): Unit = {
    result match {
      case JsSuccess(value, path) => fail(s"read should have failed and didn't - produced ${value}")
      case JsError(errors) => {
        errors.length shouldBe expectedErrors.keySet.toSeq.length

        for( error <- errors ) {
          error match {
            case (path, valErrs) => {
              expectedErrors.keySet should contain(path)
              expectedErrors(path) shouldBe valErrs
            }
          }
        }
      }
    }
  }

  def shouldHaveErrors2[T](result: JsResult[T], errorPath: JsPath, expectedError: ValidationError) = {
    result match {
      case JsSuccess(value, path) => fail(s"read should have failed and didn't - produced ${value}")
      case JsError(errors) => {
        errors.length shouldBe 1
        errors(0) match {
          case (path, error) => {
            path shouldBe errorPath
            error.length shouldBe 1
            error(0) shouldBe expectedError
          }
        }
      }
    }
  }

}
