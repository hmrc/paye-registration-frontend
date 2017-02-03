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

package controllers.userJourney

import com.google.inject.Singleton
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class WelcomeController extends WelcomeCtrl {}

trait WelcomeCtrl extends FrontendController {

  val show = Action.async { implicit request =>
    Future.successful(Ok(views.html.pages.welcome()))
  }

  val submit = Action.async { implicit request =>
    Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.tradingName()))
  }

}
