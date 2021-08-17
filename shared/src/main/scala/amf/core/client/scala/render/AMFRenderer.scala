package amf.core.client.scala.render

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.internal.plugins.render.AMFGraphRenderPlugin
import amf.core.internal.render.AMFSerializer
import org.yaml.builder.DocBuilder
object AMFRenderer {

  def render(baseUnit: BaseUnit, configuration: AMFGraphConfiguration): String =
    new AMFSerializer(baseUnit, configuration.renderConfiguration, None).renderToString

  def renderAST(baseUnit: BaseUnit, configuration: AMFGraphConfiguration): ParsedDocument =
    new AMFSerializer(baseUnit, configuration.renderConfiguration, None).renderAST

  def render(baseUnit: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): String =
    new AMFSerializer(baseUnit, configuration.renderConfiguration, Some(mediaType)).renderToString

  def renderAST(baseUnit: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): ParsedDocument =
    new AMFSerializer(baseUnit, configuration.renderConfiguration, Some(mediaType)).renderAST

  def renderGraphToBuilder[T](baseUnit: BaseUnit, builder: DocBuilder[T], conf: AMFGraphConfiguration): T = {
    // only plugin that currently supports rendering to builder interface
    AMFGraphRenderPlugin.emitToYDocBuilder(baseUnit, builder, conf.renderConfiguration)
    builder.result
  }

}
