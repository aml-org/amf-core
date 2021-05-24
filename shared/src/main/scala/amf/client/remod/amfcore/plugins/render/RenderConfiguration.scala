package amf.client.remod.amfcore.plugins.render

import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.{AMFEventListener, RenderOptions}
import amf.client.remod.amfcore.plugins.namespace.NamespaceAliasesPlugin
import amf.core.errorhandling.AMFErrorHandler

trait RenderConfiguration {
  def renderPlugins: List[AMFRenderPlugin]
  def namespacePlugins: List[NamespaceAliasesPlugin]
  def renderOptions: RenderOptions
  def errorHandler: AMFErrorHandler
  def listeners: Set[AMFEventListener]
  def syntaxPlugin: List[AMFSyntaxRenderPlugin]
}

private[amf] case class DefaultRenderConfiguration(renderPlugins: List[AMFRenderPlugin],
                                                   syntaxPlugin: List[AMFSyntaxRenderPlugin],
                                                   namespacePlugins: List[NamespaceAliasesPlugin],
                                                   renderOptions: RenderOptions,
                                                   errorHandler: AMFErrorHandler,
                                                   listeners: Set[AMFEventListener])
    extends RenderConfiguration {}

object DefaultRenderConfiguration {
  def apply(env: AMFGraphConfiguration): RenderConfiguration = {
    DefaultRenderConfiguration(
        env.registry.plugins.renderPlugins,
        env.registry.plugins.syntaxRenderPlugins,
        env.registry.plugins.namespacePlugins,
        env.options.renderOptions,
        env.errorHandlerProvider.errorHandler(),
        env.listeners
    )
  }
}
