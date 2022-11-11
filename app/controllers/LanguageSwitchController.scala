/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.AppConfig
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

import javax.inject.{Inject, Singleton}

@Singleton
class LanguageSwitchController @Inject()(appConfig: AppConfig,
                                         languageUtils: LanguageUtils,
                                         controllerComponents: ControllerComponents
                                        ) extends LanguageController(languageUtils, controllerComponents) {

  def languageMap: Map[String, Lang] = appConfig.languageMap

  def setLanguage(language: String): Action[AnyContent] = Action { implicit request =>
    val enabled: Boolean = languageMap.get(language).exists(languageUtils.isLangAvailable)
    val lang: Lang =
      if (enabled) {
        languageMap.getOrElse(language, languageUtils.getCurrentLang)
      }
      else {
        languageUtils.getCurrentLang
      }

    Redirect(fallbackURL).withLang(Lang.apply(lang.code))
  }

  protected[controllers] def fallbackURL: String = controllers.userJourney.routes.PayeStartController.startPaye.url
}
