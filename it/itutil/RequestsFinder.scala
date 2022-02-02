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

package itutil

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsValue, Json}

trait RequestsFinder {
  def getRequestBody(httpMethod: String, url: String): String = httpMethod.toLowerCase match {
    case "get" => findAll(getRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "post" => findAll(postRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "patch" => findAll(patchRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "delete" => findAll(deleteRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "put" => findAll(putRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case _ => throw new IllegalArgumentException("wrong HTTP Method")
  }

  def getGETRequestJsonBody(url: String): JsValue =
    Json.parse(getRequestBody("get", url))

  def getPUTRequestJsonBody(url: String): JsValue =
    Json.parse(getRequestBody("put", url))

  def getPOSTRequestJsonBody(url: String): JsValue =
    Json.parse(getRequestBody("post", url))

  def getPATCHRequestJsonBody(url: String): JsValue =
    Json.parse(getRequestBody("patch", url))

  def getDELETERequestJsonBody(url: String): JsValue =
    Json.parse(getRequestBody("delete", url))
}