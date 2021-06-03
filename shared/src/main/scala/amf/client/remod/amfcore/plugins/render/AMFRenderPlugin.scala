package amf.client.remod.amfcore.plugins.render

import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.model.document.BaseUnit
import org.yaml.builder.DocBuilder

object AMFRenderPlugin {
  val APPLICATION_YAML = "application/yaml"
  val APPLICATION_JSON = "application/json"
}

trait AMFRenderPlugin extends AMFPlugin[RenderInfo] {
  def defaultSyntax(): String

  def emit[T](unit: BaseUnit, builder: DocBuilder[T], renderConfiguration: RenderConfiguration): Boolean

  def mediaTypes: Seq[String]
}
