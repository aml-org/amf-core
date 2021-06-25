package amf.core.internal.plugins.render

import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.syntax.StringDocBuilder
import org.yaml.builder.DocBuilder

trait AMFRenderPlugin extends AMFPlugin[RenderInfo] {
  def defaultSyntax(): String

  def emit[T](unit: BaseUnit, builder: DocBuilder[T], renderConfiguration: RenderConfiguration): Boolean

  def emitString(unit: BaseUnit, builder: StringDocBuilder, renderConfiguration: RenderConfiguration): Boolean = {
    throw new Exception("String emission not supported")
  }

  def mediaTypes: Seq[String]
}
