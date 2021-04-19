package amf.client.remod.amfcore.plugins.render

import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.errorhandling.ErrorHandler

trait RenderConfiguration {
  def renderPlugins: List[AMFRenderPlugin]
  def renderOptions: RenderOptions
  def errorHandler: ErrorHandler
}

case class DefaultRenderConfiguration(renderPlugins: List[AMFRenderPlugin],
                                      renderOptions: RenderOptions,
                                      errorHandler: ErrorHandler)
    extends RenderConfiguration

object DefaultRenderConfiguration {
  def apply(env: AMFGraphConfiguration): RenderConfiguration = {
    DefaultRenderConfiguration(env.registry.plugins.renderPlugins,
                               env.options.renderOptions,
                               env.errorHandlerProvider.errorHandler())
  }
}
