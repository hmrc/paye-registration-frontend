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

package controllers.test

import connectors.PAYERegistrationConnector
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils._

import scala.concurrent.Future

class FeatureSwitchControllerImpl @Inject()(val featureManager: FeatureManager,
                                            val payeFeatureSwitch: PAYEFeatureSwitches,
                                            val payeRegConnector: PAYERegistrationConnector) extends FeatureSwitchController

trait FeatureSwitchController extends FrontendController {

  val featureManager: FeatureManager
  val payeFeatureSwitch: PAYEFeatureSwitches
  val payeRegConnector: PAYERegistrationConnector

  def switcher(featureName: String, featureState: String): Action[AnyContent] = Action.async {
    implicit request =>
      def updateFeature: FeatureSwitch = featureState match {
        case "true"                                      => featureManager.enable(BooleanFeatureSwitch(featureName, enabled = true))
        case "addressLookupFrontend"                     => featureManager.enable(BooleanFeatureSwitch(featureName, enabled = true))
        case x if x.matches(Validators.datePatternRegex) => featureManager.setSystemDate(ValueSetFeatureSwitch(featureName, featureState))
        case x@"time-clear"                              => featureManager.clearSystemDate(ValueSetFeatureSwitch(featureName, x))
        case _                                           => featureManager.disable(BooleanFeatureSwitch(featureName, enabled = false))
      }

      payeFeatureSwitch(featureName) match {
        case Some(f) if f.name == "system-date" =>
          val feature = updateFeature
          payeRegConnector.setBackendDate(feature.value) map {
            if(_) Ok(s"${feature.toString} set in frontend and back end") else Ok(s"${feature.toString} not set in backend something went wrong")
          }
        case Some(_)                            => Future.successful(Ok(updateFeature.toString))
        case None                               => Future.successful(BadRequest)
      }
  }

  def show: Action[AnyContent] = Action.async {
    implicit request =>
      val f = payeFeatureSwitch.all.foldLeft("")((s: String, fs: FeatureSwitch) => s + s"""${fs.name} # ${fs.enabled} # ${fs.value}\n""")
      Future.successful(Ok(f))
  }
}