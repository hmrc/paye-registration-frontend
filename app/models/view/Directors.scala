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

package models.view

import models.api.Director
import play.api.libs.json.Json


case class Directors(directorMapping: Map[String, Director])

object Directors {
  implicit val directorFormat        = Director.format
  implicit val directorMappingFormat = Json.format[Directors]
}

case class UserEnteredNino (id: String,
                            nino: Option[String])

object UserEnteredNino {
  implicit val format = Json.format[UserEnteredNino]
}

case class Ninos (ninoMapping: List[UserEnteredNino])

object Ninos {
  implicit val format = Json.format[Ninos]
}
