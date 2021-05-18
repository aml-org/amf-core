package amf.client.remod.amfcore.plugins.render

import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.{AMFEventListener, RenderOptions}
import amf.client.remod.amfcore.plugins.namespace.NamespaceAliasesPlugin
import amf.core.errorhandling.ErrorHandler

trait RenderConfiguration {
  def renderPlugins: List[AMFRenderPlugin]
  def namespacePlugins: List[NamespaceAliasesPlugin]
  def renderOptions: RenderOptions
  def errorHandler: ErrorHandler
  def listeners: Set[AMFEventListener]
}

case class DefaultRenderConfiguration(renderPlugins: List[AMFRenderPlugin],
                                      namespacePlugins: List[NamespaceAliasesPlugin],
                                      renderOptions: RenderOptions,
                                      errorHandler: ErrorHandler,
                                      listeners: Set[AMFEventListener])
    extends RenderConfiguration

object DefaultRenderConfiguration {
  def apply(env: AMFGraphConfiguration): RenderConfiguration = {
    DefaultRenderConfiguration(
        env.registry.plugins.renderPlugins,
        env.registry.plugins.namespacePlugins,
        env.options.renderOptions,
        env.errorHandlerProvider.errorHandler(),
        env.listeners
    )
  }
}
