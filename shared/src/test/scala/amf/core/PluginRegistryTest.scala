package amf.core

import amf.core.client.common.{HighPriority, NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, Document}
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SimpleReferenceHandler}
import amf.core.internal.parser.Root
import amf.core.internal.registries.PluginsRegistry
import amf.core.internal.remote.Spec
import org.scalatest.{FunSuite, Matchers}

class PluginRegistryTest extends FunSuite with Matchers {

  // TODO: Add tests for rest of plugin types ??
  test("Registering two parse plugins with same id overrides the first") {
    val firstPlugin  = DummyParsePlugin("1", "First Parse Plugin")
    val secondPlugin = DummyParsePlugin("1", "Second Parse Plugin")
    val registry     = PluginsRegistry.empty.withPlugin(firstPlugin)

    registry.parsePlugins should have size 1
    registry.parsePlugins.head.asInstanceOf[DummyParsePlugin].specName shouldBe firstPlugin.specName

    val finalRegistry = registry.withPlugin(secondPlugin)
    finalRegistry.parsePlugins should have size 1
    finalRegistry.parsePlugins.head.asInstanceOf[DummyParsePlugin].specName shouldBe secondPlugin.specName
  }
}

case class DummyParsePlugin(override val id: String, specName: String, priority: PluginPriority = NormalPriority)
    extends AMFParsePlugin {
  override def spec: Spec = Spec(specName)

  override def parse(document: Root, ctx: ParserContext): BaseUnit = Document().withId("something")

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq.empty

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = SimpleReferenceHandler

  override def referencePlugins: Seq[AMFParsePlugin] = Nil

  override def allowRecursiveReferences: Boolean = true

  override def applies(element: Root): Boolean = true
}
