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

package controllers.test

import javax.inject.{Inject, Singleton}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils._

import scala.concurrent.Future

@Singleton
class FeatureSwitchController @Inject()(
                                       injFeatureSwitch: FeatureSwitchManager,
                                       injPayeFeatureSwitch: PAYEFeatureSwitch)
  extends FeatureSwitchCtrl{
  val featureManager = injFeatureSwitch
  val PayeFeatureSwitch = injPayeFeatureSwitch

}

trait FeatureSwitchCtrl extends FrontendController {

  val featureManager : FeatureManager
  val PayeFeatureSwitch : PAYEFeatureSwitches

  def addressServiceSwitch(featureName: String, featureState: String) = Action.async {
    implicit request =>

      def feature: FeatureSwitch = featureState match {
        case "addressLookupFrontend" => featureManager.enable(BooleanFeatureSwitch(featureName, enabled = true))
        case _ => featureManager.disable(BooleanFeatureSwitch(featureName, enabled = false))
      }

      PayeFeatureSwitch(featureName) match {
        case Some(_) => Future.successful(Ok(feature.toString))
        case None => Future.successful(BadRequest)
      }
  }

}
