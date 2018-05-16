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

package models.api

import models.external.CurrentProfile
import play.api.Logger
import play.api.libs.json.{Format, JsValue, Json, Reads}

case class SessionMap(sessionId: String, registrationId: String, transactionId: String, data: Map[String, JsValue]) extends Product with Serializable {
  def getEntry[T](key: String)(implicit reads: Reads[T]): Option[T] = data.get(key).flatMap(_.asOpt[T])
  def store[A](key: String, value: A)(implicit format: Format[A]): SessionMap = this copy (data = this.data + (key -> Json.toJson(value)))
}

object SessionMap {
  implicit val format = Json.format[SessionMap]

  def apply(sessionId: String, cp: CurrentProfile, data: Map[String, JsValue]): SessionMap = new SessionMap(sessionId, cp.registrationID, cp.companyTaxRegistration.transactionId, data)
  def apply[T](sessionId: String, cp: CurrentProfile, formId: String, data: T)(implicit format: Format[T]): SessionMap =
    new SessionMap(sessionId, cp.registrationID, cp.companyTaxRegistration.transactionId, Map[String, JsValue]()).store(formId, data)
}
