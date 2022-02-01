package amf.core.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.parser.{AMFCompiler, CompilerConfiguration, CompilerContextBuilder}
import amf.core.internal.remote.InternalContent
import amf.core.internal.unsafe.PlatformSecrets
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

case class CompilerConfigTest() extends AsyncFunSuite with PlatformSecrets with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  class CustomContentUrlResourceLoader(customUrl: Option[String]) extends ResourceLoader {
    override def fetch(resource: String): Future[Content] = Future.successful(
        customUrl match {
          case Some(defined) => new Content("with custom url".stripMargin, defined)
          case None          => new Content("without url") // new constructor where no url has to be passed.
        }
    )
    override def accepts(resource: String): Boolean = true
  }

  test("fetch content with resource loader that returns custom url") {
    val url          = "file://some/url.json"
    val customUrl    = "file://some/other/url.json"
    val customLoader = new CustomContentUrlResourceLoader(Some(customUrl))
    obtainContentFromConfig(url, customLoader).map(_.url shouldBe customUrl)
  }

  test("fetch content with resource loader that returns no url") {
    val url          = "file://some/url.json"
    val customLoader = new CustomContentUrlResourceLoader(None)
    obtainContentFromConfig(url, customLoader).map(_.url shouldBe url)
  }

  def obtainContentFromConfig(url: String, rl: ResourceLoader): Future[InternalContent] = {
    val config = AMFGraphConfiguration.predefined().withResourceLoaders(List(rl))
    CompilerConfiguration(config).resolveContent(url)
  }
}
