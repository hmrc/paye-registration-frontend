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

package views

import helpers.{PayeComponentSpec, PayeFakedApp}
import org.apache.commons.io.IOUtils
import play.api.i18n.{I18nSupport, Lang, MessagesApi}

import scala.io.{BufferedSource, Source}

class WelshLanguageSpec extends PayeComponentSpec with PayeFakedApp with I18nSupport {

   val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "the message files" should {

    "have same English keys as Welsh message keys" in withEnglishAndWelshMessages { (messageKeysEnglish: List[String], messageKeysWelsh: List[String]) =>
      messageKeysEnglish.foreach(englishKey => messageKeysWelsh must contain(englishKey))
    }

    "have same Welsh keys as English message keys" in withEnglishAndWelshMessages { (messageKeysEnglish: List[String], messageKeysWelsh: List[String]) =>
      messageKeysWelsh.foreach(welshKey => messageKeysEnglish must contain(welshKey))
    }

    "retrieve correct welsh text" in {
      messagesApi("service.name")(Lang("cy")) mustBe "Cofrestru fel cyflogwr ar gyfer TWE"
    }
  }

  private def withEnglishAndWelshMessages(testCode: (List[String], List[String]) => Any): Any = {
    val englishMessages: BufferedSource = Source.fromResource("messages.en")
    val welshMessages: BufferedSource = Source.fromResource("messages.cy")
    val messageKeysEnglish: List[String] = getMessageKeys(englishMessages).toList
    val messageKeysWelsh: List[String] = getMessageKeys(welshMessages).toList
    try {
      testCode(messageKeysEnglish, messageKeysWelsh)
    } finally {
      englishMessages.close()
      welshMessages.close()
    }
  }

  private def getMessageKeys(source: Source): Iterator[String] =
    source
      .getLines
      .map(_.trim)
      .filter(!_.startsWith("#"))
      .filter(_.nonEmpty)
      .map(_.split(' ').head)
}
