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

package views.pages

import helpers.{PayeComponentSpec, PayeFakedApp}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import services.ThresholdService
import views.html.pages.{welcome => Welcome}

class WelcomePageSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {
  implicit val request                        = FakeRequest()
  implicit lazy val messagesApi : MessagesApi = mockMessagesApi
  lazy val thresholdService                   = app.injector.instanceOf[ThresholdService]

  override def afterAll(): Unit = {
    super.afterAll()
    System.setProperty("feature.system-date", "")
  }

  "Welcome" should {
    "display the 2017 PAYE thresholds" when {
      "the system date is before the 6 Apr 2018" in {
        System.setProperty("feature.system-date", "2018-04-05")

        lazy val welcomeView = Welcome(thresholdService.getCurrentThresholds)
        lazy val document    = Jsoup.parse(welcomeView.body)

        document.getElementById("pay").text mustBe messagesApi("pages.welcome.bullet.pay", 113, 490, 5876)
      }
    }

    "display the 2018 PAYE thresholds" when {
      "the system date is on the 6 Apr 2018" in {
        System.setProperty("feature.system-date", "2018-04-06")

        lazy val welcomeView = Welcome(thresholdService.getCurrentThresholds)
        lazy val document    = Jsoup.parse(welcomeView.body)

        document.getElementById("pay").text mustBe messagesApi("pages.welcome.bullet.pay", 116, 503, 6032)
      }

      "the system date is after the 6 Apr 2018" in {
        System.setProperty("feature.system-date", "2018-04-10")

        lazy val welcomeView = Welcome(thresholdService.getCurrentThresholds)
        lazy val document    = Jsoup.parse(welcomeView.body)

        document.getElementById("pay").text mustBe messagesApi("pages.welcome.bullet.pay", 116, 503, 6032)
      }
    }
  }
}
