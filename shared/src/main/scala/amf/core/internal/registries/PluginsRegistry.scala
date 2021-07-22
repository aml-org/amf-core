package amf.core.internal.registries

import amf.core.client.scala.parse.{AMFParsePlugin, AMFSyntaxParsePlugin}
import amf.core.client.scala.render.{AMFElementRenderPlugin, AMFSyntaxRenderPlugin}
import amf.core.client.scala.validation.payload.AMFShapePayloadValidationPlugin
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.namespace.NamespaceAliasesPlugin
import amf.core.internal.plugins.parse.{DomainParsingFallback, ExternalFragmentDomainFallback}
import amf.core.internal.plugins.render.AMFRenderPlugin
import amf.core.internal.plugins.validation.AMFValidatePlugin

/**
  * A registry of plugins
  *
  * @param parsePlugins          a list of [[AMFParsePlugin]]
  * @param validatePlugins       a list of [[AMFValidatePlugin]]
  * @param renderPlugins         a list of [[AMFRenderPlugin]]
  * @param domainParsingFallback [[DomainParsingFallback]]
  */
case class PluginsRegistry private[amf] (parsePlugins: List[AMFParsePlugin],
                                         validatePlugins: List[AMFValidatePlugin],
                                         renderPlugins: List[AMFRenderPlugin],
                                         payloadPlugins: List[AMFShapePayloadValidationPlugin],
                                         namespacePlugins: List[NamespaceAliasesPlugin],
                                         syntaxParsePlugins: List[AMFSyntaxParsePlugin],
                                         syntaxRenderPlugins: List[AMFSyntaxRenderPlugin],
                                         elementRenderPlugins: List[AMFElementRenderPlugin],
                                         domainParsingFallback: DomainParsingFallback) {

  lazy val allPlugins: List[AMFPlugin[_]] = parsePlugins ++ validatePlugins ++ renderPlugins ++ payloadPlugins ++
    namespacePlugins ++ syntaxParsePlugins ++ syntaxRenderPlugins ++ elementRenderPlugins

  def withPlugin(plugin: AMFPlugin[_]): PluginsRegistry = {
    plugin match {
      case p: AMFParsePlugin if !parsePlugins.exists(_.id == p.id) =>
        copy(parsePlugins = parsePlugins :+ p)
      case v: AMFValidatePlugin if !validatePlugins.exists(_.id == v.id) =>
        copy(validatePlugins = validatePlugins :+ v)
      case r: AMFRenderPlugin if !renderPlugins.exists(_.id == r.id) =>
        copy(renderPlugins = renderPlugins :+ r)
      case r: NamespaceAliasesPlugin if !namespacePlugins.exists(_.id == r.id) =>
        copy(namespacePlugins = namespacePlugins :+ r)
      case r: AMFSyntaxParsePlugin if !syntaxParsePlugins.exists(_.id == r.id) =>
        copy(syntaxParsePlugins = syntaxParsePlugins :+ r)
      case r: AMFSyntaxRenderPlugin if !syntaxRenderPlugins.exists(_.id == r.id) =>
        copy(syntaxRenderPlugins = syntaxRenderPlugins :+ r)
      case r: AMFShapePayloadValidationPlugin if !payloadPlugins.exists(_.id == r.id) =>
        copy(payloadPlugins = payloadPlugins :+ r)
      case r: AMFElementRenderPlugin if !elementRenderPlugins.exists(_.id == r.id) =>
        copy(elementRenderPlugins = elementRenderPlugins :+ r)
      case _ => this
    }
  }

  def withPlugins(plugins: Seq[AMFPlugin[_]]): PluginsRegistry = {
    plugins.foldLeft(this) { case (registry, plugin) => registry.withPlugin(plugin) }
  }

  def removePlugin(id: String): PluginsRegistry =
    copy(
        parsePlugins = parsePlugins.filterNot(_.id == id),
        validatePlugins = validatePlugins.filterNot(_.id == id),
        renderPlugins = renderPlugins.filterNot(_.id == id),
        namespacePlugins = namespacePlugins.filterNot(_.id == id)
    )

}

object PluginsRegistry {

  /** Creates an empty PluginsRegistry */
  val empty: PluginsRegistry = PluginsRegistry(Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, ExternalFragmentDomainFallback)
}
