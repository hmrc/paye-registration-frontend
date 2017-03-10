package itutil

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsString, JsObject, Json}
import uk.gov.hmrc.crypto.json.JsonEncryptor
import uk.gov.hmrc.crypto.{ApplicationCrypto, Protected}

trait CachingStub {

  implicit lazy val jsonCrypto = ApplicationCrypto.JsonCrypto
  implicit lazy val encryptionFormat = new JsonEncryptor[JsObject]()

  def stubKeystoreMetadata(session: String, regId: String, companyName: String) = {
    val keystoreUrl = s"/keystore/paye-registration-frontend/${session}"
    stubFor(get(urlMatching(keystoreUrl))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
               |"id": "${session}",
               |"data": {
               | "CurrentProfile": {
               |   "registrationID": "${regId}",
               |   "completionCapacity": "Director",
               |   "companyTaxRegistration": {
               |      "status": "submitted",
               |      "transactionId": "12345"
               |   },
               |   "language": "ENG"
               |  },
               |  "CoHoCompanyDetails": {
               |    "registration_id": "${regId}",
               |    "company_name": "${companyName}",
               |    "areas_of_industry": []
               |  }
               |}
               |}""".stripMargin
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
