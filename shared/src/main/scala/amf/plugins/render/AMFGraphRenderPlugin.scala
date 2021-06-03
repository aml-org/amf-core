package amf.plugins.render

import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.render.AMFRenderPlugin.APPLICATION_JSON
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.core.model.document.BaseUnit
import amf.core.remote.Amf
import amf.core.vocabulary.Namespace
import amf.plugins.document.graph.emitter.EmbeddedJsonLdEmitter
import org.yaml.builder.DocBuilder

object AMFGraphRenderPlugin extends AMFRenderPlugin {

  override val id: String = Amf.name

  override def defaultSyntax(): String = APPLICATION_JSON

  override def emit[T](unit: BaseUnit, builder: DocBuilder[T], renderConfiguration: RenderConfiguration): Boolean = {
    val namespaceAliases = renderConfiguration.namespacePlugins.sorted
      .find(_.applies(unit))
      .map(_.aliases(unit))
      .getOrElse(Namespace.defaultAliases)
    EmbeddedJsonLdEmitter.emit(unit, builder, renderConfiguration.renderOptions, namespaceAliases)
  }

  override def mediaTypes: Seq[String] = Seq(
      Amf.mediaType,
      "application/ld+json",
      "application/json",
      "application/amf+json",
      "application/amf+json",
      "application/graph"
  )

  override def applies(element: RenderInfo): Boolean = true

  override def priority: PluginPriority = NormalPriority
}
