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

package controllers.exceptions

import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{Request, Result}
import play.api.mvc.Results._
import views.html.pages.error.restart

sealed trait FrontendException extends Exception {
  def view: Option[_] = None
  def action(implicit request: Request[_], messages: Messages): Result
  def message: String
}

sealed trait RestartException extends FrontendException {
  override def view: Option[restart.type] = Some(restart)
  def action(implicit request: Request[_], messages: Messages): Result = {
    Logger.error(message)
    view.fold[Result](InternalServerError)(restart => InternalServerError(restart()))
  }
}

case class UnexpectedException(message: String) extends RestartException
case class MissingBlockInformationException(message: String) extends RestartException
