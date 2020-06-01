
package itutil

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsValue, Json}

trait RequestsFinder {
  def getRequestBody(httpMethod: String, url: String): String = httpMethod.toLowerCase match {
    case "get"    => findAll(getRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "post"   => findAll(postRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "patch"  => findAll(patchRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "delete" => findAll(deleteRequestedFor(urlMatching(url))).get(0).getBodyAsString
    case "put"    => findAll(putRequestedFor(urlMatching(url))).get(0).getBodyAsString
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