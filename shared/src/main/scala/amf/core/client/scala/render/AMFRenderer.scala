package amf.core.client.scala.render

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.internal.plugins.render.AMFGraphRenderPlugin
import amf.core.internal.remote.Mimes
import amf.core.internal.remote.Mimes.`application/ld+json`
import amf.core.internal.remote.SpecId.AMF
import amf.core.internal.render.AMFSerializer
import org.yaml.builder.DocBuilder
object AMFRenderer {

  def render(baseUnit: BaseUnit, configuration: AMFGraphConfiguration): String =
    render(baseUnit, `application/ld+json`, configuration)

  def renderAST(baseUnit: BaseUnit, configuration: AMFGraphConfiguration): ParsedDocument =
    renderAST(baseUnit, `application/ld+json`, configuration)

  def render(baseUnit: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): String =
    new AMFSerializer(baseUnit, mediaType, configuration.renderConfiguration).renderToString

  def renderAST(baseUnit: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): ParsedDocument =
    new AMFSerializer(baseUnit, mediaType, configuration.renderConfiguration).renderAST

  def renderGraphToBuilder[T](baseUnit: BaseUnit, builder: DocBuilder[T], conf: AMFGraphConfiguration): T = {
    // only plugin that currently supports rendering to builder interface
    AMFGraphRenderPlugin.emit(baseUnit, builder, conf.renderConfiguration)
    builder.result
  }

}
