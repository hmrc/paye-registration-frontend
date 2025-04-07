/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.validation._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

import java.text.Normalizer.{Form, normalize}
import scala.util.matching.Regex

object Formatters {
  val specialCharacterConverts = Map('æ' -> "ae", 'Æ' -> "AE", 'œ' -> "oe", 'Œ' -> "OE", 'ß' -> "ss", 'ø' -> "o", 'Ø' -> "O")

  def normaliseString(string: String, charFilter: Option[Regex] = None, extraSpecialCharacters: Map[Char, String] = Map()): String = {
    normalize(string, Form.NFKD)
      .replaceAll("\\p{M}", "")
      .map(char => extraSpecialCharacters.getOrElse(char, char.toString))
      .mkString
      .replaceAll(charFilter.fold("")(_.toString), "")
      .trim
  }

  lazy val normalizeTrimmedReads: Reads[String] = Reads.StringReads.map(s => normaliseString(s, extraSpecialCharacters = specialCharacterConverts))
  lazy val normalizeTrimmedListReads: Reads[List[String]] = Reads.list[String](normalizeTrimmedReads)
  lazy val normalizeTrimmedHMRCReads: Reads[String] = Reads.StringReads.map(s => normaliseString(s, Some("[^A-Za-z 0-9\\-']".r)))
  lazy val normalizeTrimmedTitleHMRCReads: Reads[String] = Reads.StringReads.map(s => normaliseString(s, Some("[^A-Za-z \\-']".r)))
  lazy val normalizeTrimmedHMRCAddressReads: Reads[String] = Reads.StringReads.map(s => normaliseString(s, Some("""[^a-zA-Z0-9, .\(\)/&'\"\-\\]""".r)))

  def ninoFormatter(nino: String): String = nino.grouped(2).mkString(" ")

  def intMapReads[V]()(implicit formatV: Format[V]): Reads[Map[Int, V]] = new Reads[Map[Int, V]] {
    def reads(jv: JsValue): JsResult[Map[Int, V]] = {
      JsSuccess(jv.as[Map[String, JsValue]].map { case (k, v) =>
        k.toInt -> v.as[V]
      })
    }
  }

  def intMapWrites[V]()(implicit formatV: Format[V]): Writes[Map[Int, V]] = new Writes[Map[Int, V]] {
    def writes(map: Map[Int, V]): JsValue =
      Json.obj(map.map { case (s, o) =>
        val ret: (String, JsValueWrapper) = s.toString -> Json.toJson[V](o)
        ret
      }.toSeq: _*)
  }

  def phoneNoReads(errMsg: String): Reads[String] = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = {
      normalizeTrimmedReads.reads(json) flatMap { input =>
        Validators.isValidPhoneNo(input) match {
          case Right(phone) => JsSuccess(phone)
          case Left(err) => JsError(err)
        }
      }
    }
  }

  lazy val emailReads = Reads.StringReads.filter(JsonValidationError("Invalid email")) {
    email =>
      Validators.emailValidation(normaliseString(email)) match {
        case e: Invalid => false
        case _ => true
      }
  }
}