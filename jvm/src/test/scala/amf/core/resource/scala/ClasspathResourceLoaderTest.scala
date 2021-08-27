package amf.core.resource.scala

import amf.core.client.common.{HighPriority, PluginPriority}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, Document}
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SimpleReferenceHandler}
import amf.core.client.scala.resource.ClasspathResourceLoader
import amf.core.internal.parser.Root
import amf.core.internal.remote.Mimes.`application/yaml`
import amf.core.internal.remote.{Mimes, Spec}
import org.scalatest.{AsyncFunSuite, Matchers}

class ClasspathResourceLoaderTest extends AsyncFunSuite with Matchers {

  test("The class resource loader can fetch a file from class jar") {
    val config = AMFGraphConfiguration
      .predefined()
      .withResourceLoader(ClasspathResourceLoader)
      .withPlugin(DummyParsePlugin)

    for {
      amfResult <- config.baseUnitClient().parse("/classLoader/api.raml")
    } yield {
      amfResult.conforms shouldBe true
      amfResult.baseUnit.id shouldEqual "something"
    }
  }
}

object DummyParsePlugin extends AMFParsePlugin {
  override def spec: Spec = Spec.AMF

  override def parse(document: Root, ctx: ParserContext): BaseUnit =
    Document()
      .withRaw(document.raw)
      .withId("something")

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(`application/yaml`)

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = SimpleReferenceHandler

  override def allowRecursiveReferences: Boolean = false

  override def applies(element: Root): Boolean = true

  override def priority: PluginPriority = HighPriority

  override def withIdAdoption: Boolean = false
}
