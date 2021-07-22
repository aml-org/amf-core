package amf.core.client.scala.render

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.internal.plugins.AMFPlugin
import org.yaml.builder.DocBuilder

trait AMFElementRenderPlugin extends AMFPlugin[DomainElement] {

  override def applies(element: DomainElement): Boolean

  def render(element: DomainElement, errorHandler: AMFErrorHandler): ParsedDocument

  override def priority: PluginPriority = NormalPriority
}
