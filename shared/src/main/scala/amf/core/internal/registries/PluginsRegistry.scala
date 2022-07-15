package amf.core.internal.registries

import amf.core.client.scala.parse.{AMFParsePlugin, AMFSyntaxParsePlugin}
import amf.core.client.scala.render.{AMFElementRenderPlugin, AMFSyntaxRenderPlugin}
import amf.core.client.scala.validation.payload.AMFShapePayloadValidationPlugin
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.parse.{DomainParsingFallback, ExternalFragmentDomainFallback}
import amf.core.internal.plugins.render.AMFRenderPlugin
import amf.core.internal.plugins.validation.AMFValidatePlugin

/** A registry of plugins
  *
  * @param parsePlugins
  *   a list of [[AMFParsePlugin]]
  * @param validatePlugins
  *   a list of [[AMFValidatePlugin]]
  * @param renderPlugins
  *   a list of [[AMFRenderPlugin]]
  * @param domainParsingFallback
  *   [[DomainParsingFallback]]
  */
case class PluginsRegistry private[amf] (
    rootParsePlugins: List[AMFParsePlugin] = Nil,
    referenceParsePlugins: List[AMFParsePlugin] = Nil,
    validatePlugins: List[AMFValidatePlugin] = Nil,
    renderPlugins: List[AMFRenderPlugin] = Nil,
    payloadPlugins: List[AMFShapePayloadValidationPlugin] = Nil,
    syntaxParsePlugins: List[AMFSyntaxParsePlugin] = Nil,
    syntaxRenderPlugins: List[AMFSyntaxRenderPlugin] = Nil,
    elementRenderPlugins: List[AMFElementRenderPlugin] = Nil,
    domainParsingFallback: DomainParsingFallback
) {

  def withPlugin(plugin: AMFPlugin[_]): PluginsRegistry = {
    plugin match {
      case p: AMFParsePlugin =>
        val updatedRootPlugins      = rootParsePlugins.filter(_.id != p.id) :+ p
        val updatedReferencePlugins = referenceParsePlugins.filter(_.id != p.id) :+ p
        copy(rootParsePlugins = updatedRootPlugins, referenceParsePlugins = updatedReferencePlugins)
      case v: AMFValidatePlugin =>
        copy(validatePlugins = validatePlugins.filter(_.id != v.id) :+ v)
      case r: AMFRenderPlugin =>
        copy(renderPlugins = renderPlugins.filter(_.id != r.id) :+ r)
      case r: AMFSyntaxParsePlugin =>
        copy(syntaxParsePlugins = syntaxParsePlugins.filter(_.id != r.id) :+ r)
      case r: AMFSyntaxRenderPlugin =>
        copy(syntaxRenderPlugins = syntaxRenderPlugins.filter(_.id != r.id) :+ r)
      case r: AMFShapePayloadValidationPlugin =>
        copy(payloadPlugins = payloadPlugins.filter(_.id != r.id) :+ r)
      case r: AMFElementRenderPlugin =>
        copy(elementRenderPlugins = elementRenderPlugins.filter(_.id != r.id) :+ r)
      case _ => this
    }
  }

  def withRootParsePlugin(plugin: AMFParsePlugin): PluginsRegistry = {
    copy(rootParsePlugins = rootParsePlugins.filter(_.id != plugin.id) :+ plugin)
  }

  def withReferenceParsePlugin(plugin: AMFParsePlugin): PluginsRegistry = {
    copy(referenceParsePlugins = referenceParsePlugins.filter(_.id != plugin.id) :+ plugin)
  }

  def withPlugins(plugins: Seq[AMFPlugin[_]]): PluginsRegistry = {
    plugins.foldLeft(this) { case (registry, plugin) => registry.withPlugin(plugin) }
  }

  def withFallback(plugin: DomainParsingFallback): PluginsRegistry = {
    copy(domainParsingFallback = plugin)
  }

  def removeAllPlugins(): PluginsRegistry = copy(Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, domainParsingFallback)
}

object PluginsRegistry {

  /** Creates an empty PluginsRegistry */
  val empty: PluginsRegistry = PluginsRegistry(domainParsingFallback = ExternalFragmentDomainFallback())
}
