/*
 * Copyright 2016 HM Revenue & Customs
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

package common.exceptions

object InternalExceptions extends InternalExceptions

trait InternalExceptions {

  class UnableToCreateEnumException(val enumName: String, attemptedString: String) extends Exception(
    s"Couldn't create enum $enumName from input $attemptedString"
  )

  class ExpectedFormFieldNotPopulatedException(val formName: String, field: String) extends Exception(
    s"field $field not populated when extracting data from $formName form"
  )

}
