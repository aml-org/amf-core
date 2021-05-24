package amf.client.remod.amfcore.plugins.render

import amf.client.plugins.AMFDocumentPlugin
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.{AMFPlugin, PluginPriority}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import org.yaml.builder.DocBuilder

trait AMFRenderPlugin extends AMFPlugin[RenderInfo] {
  def defaultSyntax(): String

  def emit[T](unit: BaseUnit,
              builder: DocBuilder[T],
              renderOptions: RenderOptions,
              errorHandler: AMFErrorHandler): Boolean

  def mediaTypes: Seq[String]
}

private[amf] case class AMFRenderPluginAdapter(plugin: AMFDocumentPlugin, override val defaultSyntax: String)
    extends AMFRenderPlugin {

  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: AMFErrorHandler): Boolean =
    plugin.emit(unit, builder, renderOptions, errorHandler)

  override val id: String = plugin.ID

  override val mediaTypes: Seq[String] = plugin.documentSyntaxes

  override def applies(renderingInfo: RenderInfo): Boolean = {
    plugin.canUnparse(renderingInfo.unit)
  }

  override def priority: PluginPriority = PluginPriority(plugin.priority)
}
