import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "paye-registration-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val frontendBootstrapVersion        = "8.8.0"
  private val playPartialsVersion             = "6.1.0"
  private val httpCachingVersion              = "7.0.0"
  private val playWhitelistVersion            = "2.0.0"
  private val playConditionalMappingVersion   = "0.2.0"

  private val scalaTestPlusVersion     = "2.0.1"
  private val hmrcTestVersion          = "2.3.0"
  private val scalaTestVersion         = "3.0.1"
  private val pegdownVersion           = "1.6.0"
  private val mockitoCoreVersion       = "2.7.17"
  private val jsoupVersion             = "1.10.2"
  private val reactiveMongoTestVersion = "2.0.0"
  private val wireMockVersion          = "2.5.1"


  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap"             % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials"                  % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client"            % httpCachingVersion,
    "uk.gov.hmrc" %% "play-whitelist-filter"          % playWhitelistVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping"  % playConditionalMappingVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"             %% "hmrctest"           % hmrcTestVersion           % scope,
        "org.scalatest"           %% "scalatest"          % scalaTestVersion          % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion      % scope,
        "org.pegdown"             %  "pegdown"            % pegdownVersion            % scope,
        "org.jsoup"               %  "jsoup"              % jsoupVersion              % scope,
        "com.typesafe.play"       %% "play-test"          % PlayVersion.current       % scope,
        "uk.gov.hmrc"             %% "reactivemongo-test" % reactiveMongoTestVersion  % scope,
        "org.mockito"             %  "mockito-core"       % mockitoCoreVersion        % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc"             %% "hmrctest"           % hmrcTestVersion           % scope,
        "org.scalatest"           %% "scalatest"          % scalaTestVersion          % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion      % scope,
        "org.pegdown"             %  "pegdown"            % pegdownVersion            % scope,
        "org.jsoup"               %  "jsoup"              % jsoupVersion              % scope,
        "com.typesafe.play"       %% "play-test"          % PlayVersion.current       % scope,
        "uk.gov.hmrc"             %% "reactivemongo-test" % reactiveMongoTestVersion  % scope,
        "com.github.tomakehurst"  %  "wiremock"           % wireMockVersion           % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
