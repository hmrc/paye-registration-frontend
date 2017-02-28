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

import models.view.{CompletionCapacity => CompletionCapacityView}
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings._
import utils.Validators.nonEmpty

object CompletionCapacityForm {

  private def ifOther(mapping: Mapping[String]): Mapping[String] =
    onlyIf(isEqual("completionCapacity", "other"), mapping)("")

  val form = Form(
    mapping(
      "completionCapacity" -> text,
      "completionCapacityOther" -> ifOther(text.verifying(nonEmpty))
    )(CompletionCapacityView.apply)(CompletionCapacityView.unapply)
  )
}
