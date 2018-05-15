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

package itutil

import com.github.tomakehurst.wiremock.client.WireMock._
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import repositories.ReactiveMongoRepository
import uk.gov.hmrc.crypto.json.JsonEncryptor
import uk.gov.hmrc.crypto.{ApplicationCrypto, Protected}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.ExecutionContext.Implicits.global

trait CachingStub extends PlaySpec with MongoSpecSupport {

  implicit lazy val jsonCrypto = ApplicationCrypto.JsonCrypto
  implicit lazy val encryptionFormat = new JsonEncryptor[JsObject]()

  import scala.concurrent.duration._
  def customAwait[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  def stubKeystoreMetadata(session: String, regId: String, submitted: Boolean = false)(implicit app: Application) = {

    val repo = new ReactiveMongoRepository(app.configuration, mongo)
    val defaultTimeout: FiniteDuration = 5 seconds

    customAwait(repo.ensureIndexes)(defaultTimeout)
    customAwait(repo.drop)(defaultTimeout)

    val preawait = customAwait(repo.count)(defaultTimeout)
    val cp = CurrentProfile(
      registrationID            = regId,
      companyTaxRegistration    =
        CompanyRegistrationProfile(
          status = "submitted",
          transactionId = "12345"),
      language                  = "ENG",
      payeRegistrationSubmitted = submitted
    )
    val currentProfileMapping: Map[String, JsValue] = Map("CurrentProfile" -> Json.toJson(cp))
    val res = customAwait(repo.upsert(CacheMap(session, currentProfileMapping)))(defaultTimeout)
    customAwait(repo.count)(defaultTimeout) mustBe preawait + 1
    res
  }

  def stubPayeRegDocumentStatus(regId: String) = {
    val payeRegUrl = s"/paye-registration/$regId/status"
    stubFor(get(urlMatching(payeRegUrl))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "status" : "draft"
               |}
             """.stripMargin
          )
      )
    )
  }

  def stubS4LGet(regId: String, key: String, data: String) = {
    val s4lData = Json.parse(data).as[JsObject]
    val encData = encryptionFormat.writes(Protected(s4lData)).as[JsString]

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(get(urlMatching(s"/save4later/paye-registration-frontend/$regId"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }

  def stubS4LPut(regId: String, key: String, data: String) = {
    val s4lData = Json.parse(data).as[JsObject]
    val encData = encryptionFormat.writes(Protected(s4lData)).as[JsString]

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(put(urlMatching(s"/save4later/paye-registration-frontend/$regId/data/$key"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }

}
