package amf.client.remod.amfcore.plugins.render

import amf.client.plugins.AMFDocumentPlugin
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.{AMFPlugin, PluginPriority}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import org.yaml.builder.DocBuilder

trait AMFRenderPlugin extends AMFPlugin[RenderInfo] {
  def emit[T](unit: BaseUnit,
              builder: DocBuilder[T],
              renderOptions: RenderOptions,
              errorHandler: AMFErrorHandler): Boolean
}

private[amf] case class AMFRenderPluginAdapter(plugin: AMFDocumentPlugin) extends AMFRenderPlugin {

  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: AMFErrorHandler): Boolean =
    plugin.emit(unit, builder, renderOptions, errorHandler)

  override val id: String = plugin.ID

  override def applies(renderingInfo: RenderInfo): Boolean = {
    plugin.vendors.contains(renderingInfo.vendor) &&
    plugin.documentSyntaxes.contains(renderingInfo.mediaType) &&
    plugin.canUnparse(renderingInfo.unit)
  }

  override def priority: PluginPriority = PluginPriority(plugin.priority)
}
