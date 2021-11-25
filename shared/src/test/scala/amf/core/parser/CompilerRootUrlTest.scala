package amf.core.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.unsafe.PlatformSecrets
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class CompilerRootUrlTest extends AsyncFunSuite with PlatformSecrets with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  class CustomContentUrlResourceLoader(customUrl: String) extends ResourceLoader {
    override def fetch(resource: String): Future[Content] = Future.successful(
        new Content("""
          |{
          |   "a": 5
          |}""".stripMargin,
                    customUrl)
    )
    override def accepts(resource: String): Boolean = true
  }

  test("Location of Root matches location in SyamlParsedDocument when resource loader returns custom url") {
    val url          = "file://some/url.json"
    val customUrl    = "file://some/other/url.json"
    val customLoader = new CustomContentUrlResourceLoader(customUrl)

    val config = AMFGraphConfiguration.predefined().withResourceLoaders(List(customLoader))
    val context =
      new CompilerContextBuilder(url, config.registry.sortedParsePlugins, platform, config.compilerConfiguration)
        .build()

    new AMFCompiler(context).root().map { root =>
      val document = root.parsed.asInstanceOf[SyamlParsedDocument]
      root.location shouldBe customUrl
      root.location shouldBe document.document.location.sourceName
    }
  }
}
