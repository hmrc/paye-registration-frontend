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

package utils

import java.text.Normalizer

import play.api.libs.json.{JsResult, JsSuccess, JsValue, Json, Reads}

object Formatters {
  def ninoFormatter(nino: String): String = nino.grouped(2).mkString(" ")

  lazy val normalizeReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json).flatMap {
      s =>
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFKD)
        JsSuccess(normalized.replaceAll("\\p{M}", ""))
    }
  }

  lazy val normalizeListReads = new Reads[List[String]] {
    override def reads(json: JsValue): JsResult[List[String]] = Json.fromJson[List[String]](json).flatMap {
      l => JsSuccess(l.map(Normalizer.normalize(_, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")))
    }
  }
}
