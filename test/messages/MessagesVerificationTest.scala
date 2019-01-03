/*
 * Copyright 2019 HM Revenue & Customs
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

package messages

import helpers.PayeComponentSpec

import scala.io.Source

class MessagesVerificationTest extends PayeComponentSpec {

  val lines = Source.fromFile("conf/messages").getLines().toList

  private def key(line: String): String = line.takeWhile(_ != '=')
  def findErrors(regex: String) = lines.map(line => if(line.matches(regex)) key(line.trim) else "").distinct.filterNot(_.eq(""))

  "Messages file" should {
    "not contain unescaped single quotes" in {
      val testRegex = """^.+[=].*[^']'[^'].*$"""

      val errorList = findErrors(testRegex)

      if(errorList.nonEmpty) {
        fail(s"Found an unescaped single quotes in messages file under keys 👇 ${"\n"} ${errorList.mkString("\n")}")
      }
    }

    "not contain attribute values unenclosed in double quotes" ignore {
      val testRegex = """^.+=.+=\s*[{].*$"""

      val errorList = findErrors(testRegex)

      if(errorList.nonEmpty) {
        fail(s"Found an open brace not preceded by double quotes, but preceded by an equals sign for keys 👇 ${"\n"}  ${errorList.mkString("\n")}")
      }
    }
  }
}

