package services

import connectors.{BusinessRegistrationConnector, PAYERegistrationConnector}
import fixtures.{CoHoAPIFixture, PAYERegistrationFixture, S4LFixture}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

class PrepopulationServiceSpec extends PAYERegSpec {
  implicit val hc = HeaderCarrier()
  val returnHttpResponse = HttpResponse(200)

        class Setup {
          val service = new PrepopulationSrv {
            override val busRegConnector = mockBusinessRegistrationConnector
          }
      }
}
