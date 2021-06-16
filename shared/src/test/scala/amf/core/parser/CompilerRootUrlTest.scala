package amf.core.parser

import amf.Core
import amf.client.remote.Content
import amf.core.{AMF, AMFCompiler, CompilerContextBuilder}
import amf.core.services.RuntimeCompiler
import amf.core.unsafe.PlatformSecrets
import amf.internal.environment.Environment
import amf.internal.resource.ResourceLoader
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

    AMF.init().flatMap { _ =>
      val env = Environment.empty().withLoaders(List(customLoader))
      val context = new CompilerContextBuilder(url, platform)
        .withEnvironment(env)
        .build()

      new AMFCompiler(context, None, None).root().map { root =>
        val document = root.parsed.asInstanceOf[SyamlParsedDocument]
        root.location shouldBe customUrl
        root.location shouldBe document.document.location.sourceName
      }
    }
  }
}
