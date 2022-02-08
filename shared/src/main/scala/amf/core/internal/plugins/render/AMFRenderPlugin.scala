package amf.core.internal.plugins.render

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.syntax.ASTBuilder

trait AMFRenderPlugin extends AMFPlugin[RenderInfo] {
  def defaultSyntax(): String

  final def emit(unit: BaseUnit, renderConfiguration: RenderConfiguration, mediaType: String): ParsedDocument = {
    val builder = getDefaultBuilder
    emit(unit, builder, renderConfiguration, mediaType)
    builder.parsedDocument
  }

  def emit[T](unit: BaseUnit,
              builder: ASTBuilder[T],
              renderConfiguration: RenderConfiguration,
              mediaType: String): Boolean

  def mediaTypes: Seq[String]

  def getDefaultBuilder: ASTBuilder[_]
}
