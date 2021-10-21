package amf.core.internal.plugins.render

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.vocabulary.{Namespace, NamespaceAliases}
import amf.core.internal.plugins.document.graph.emitter.{EmbeddedJsonLdEmitter, FlattenedJsonLdEmitter}
import amf.core.internal.plugins.document.graph.{EmbeddedForm, JsonLdSerialization}
import amf.core.internal.plugins.syntax.ASTBuilder
import amf.core.internal.remote.Amf
import amf.core.internal.remote.Mimes._
import org.yaml.builder.DocBuilder

object AMFGraphRenderPlugin extends AMFRenderPlugin {

  override val id: String = Amf.id

  override def defaultSyntax(): String = `application/json`

  override def emit[T](unit: BaseUnit, builder: ASTBuilder[T], renderConfig: RenderConfiguration): Boolean = {
    builder match {
      case yDocBuilder: DocBuilder[_] =>
        emitToYDocBuilder(unit, yDocBuilder, renderConfig)
      case _ => false
    }
  }

  def emitToYDocBuilder[T](unit: BaseUnit, builder: DocBuilder[T], renderConfig: RenderConfiguration): Boolean = {
    val options          = renderConfig.renderOptions
    val namespaceAliases = generateNamespaceAliasesFromPlugins(unit, renderConfig)
    options.toGraphSerialization match {
      case JsonLdSerialization(EmbeddedForm) => EmbeddedJsonLdEmitter.emit(unit, builder, options, namespaceAliases)
      // defaults to flatten
      case _ => FlattenedJsonLdEmitter.emit(unit, builder, options, namespaceAliases, renderConfig.extensionModels)
    }
  }

  private def generateNamespaceAliasesFromPlugins(unit: BaseUnit, config: RenderConfiguration): NamespaceAliases =
    config.namespacePlugins.sorted
      .find(_.applies(unit))
      .map(_.aliases(unit))
      .getOrElse(Namespace.defaultAliases)

  override def mediaTypes: Seq[String] = Seq(
      `application/ld+json`,
      `application/graph`
  )

  override def applies(element: RenderInfo): Boolean = true

  override def priority: PluginPriority = LowPriority

  override def getDefaultBuilder: ASTBuilder[_] = new SYAMLASTBuilder
}
