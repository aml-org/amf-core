package amf.core.internal.plugins.render

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.syntax.{ASTBuilder, StringDocBuilder}
import org.yaml.builder.DocBuilder

trait AMFRenderPlugin extends AMFPlugin[RenderInfo] {
  def defaultSyntax(): String

  final def emit(unit: BaseUnit, renderConfiguration: RenderConfiguration): ParsedDocument = {
    val builder = getDefaultBuilder
    emit(unit, builder, renderConfiguration)
    builder.parsedDocument
  }

  def emit[T](unit: BaseUnit, builder: ASTBuilder[T], renderConfiguration: RenderConfiguration): Boolean

  def mediaTypes: Seq[String]

  def getDefaultBuilder: ASTBuilder[_]
}
