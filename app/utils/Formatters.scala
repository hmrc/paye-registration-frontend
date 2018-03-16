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

package utils

import java.text.Normalizer

import play.api.data.validation._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

object Formatters {
  def ninoFormatter(nino: String): String = nino.grouped(2).mkString(" ")

  private def normalize(str: String): String = Normalizer.normalize(str, Normalizer.Form.NFKD)
    .replaceAll("\\p{M}", "")
    .trim

  lazy val normalizeTrimmedReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json) flatMap (s => JsSuccess(normalize(s)))
  }

  lazy val normalizeTrimmedHMRCReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json) flatMap {
      str => JsSuccess(normalize(str).replaceAll("[^A-Za-z 0-9\\-']", ""))
    }
  }

  lazy val normalizeTrimmedHMRCAddressReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json) flatMap {
      str => JsSuccess(normalize(str).replaceAll("""[^a-zA-Z0-9, .\(\)/&'\"\-\\]""", ""))
    }
  }

  lazy val normalizeTrimmedListReads = new Reads[List[String]] {
    override def reads(json: JsValue): JsResult[List[String]] = Json.fromJson[List[String]](json).flatMap {
      l => JsSuccess(l map (normalize _))
    }
  }

  def intMapReads[V]()(implicit formatV: Format[V]): Reads[Map[Int, V]] = new Reads[Map[Int, V]] {
    def reads(jv: JsValue): JsResult[Map[Int, V]] = {
      JsSuccess(jv.as[Map[String, JsValue]].map { case (k, v) =>
        k.toInt -> v.as[V]
      })
    }
  }

  def intMapWrites[V]()(implicit formatV: Format[V]): Writes[Map[Int, V]] = new Writes[Map[Int, V]] {
    def writes(map: Map[Int, V]): JsValue =
      Json.obj(map.map{case (s, o) =>
        val ret: (String, JsValueWrapper) = s.toString -> Json.toJson[V](o)
        ret
      }.toSeq:_*)
  }

  def phoneNoReads(errMsg: String) = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = {
      normalizeTrimmedReads.reads(json) flatMap { input =>
        Validators.isValidPhoneNo(input) match {
          case Right(phone) => JsSuccess(phone)
          case Left(err)    => JsError(err)
        }
      }
    }
  }

  lazy val emailReads = Reads.StringReads.filter(ValidationError("Invalid email")) {
    email => Validators.emailValidation(normalize(email)) match {
      case e: Invalid => false
      case _          => true
    }
  }
}
