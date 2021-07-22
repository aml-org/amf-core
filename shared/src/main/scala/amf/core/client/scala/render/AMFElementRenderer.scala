package amf.core.client.scala.render

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.parse.document.ParsedDocument

object AMFElementRenderer {

  def renderElement(element: DomainElement, config: AMFGraphConfiguration): ParsedDocument = {
    val plugins      = config.registry.sortedElementRenderPlugins
    val errorHandler = config.errorHandlerProvider.errorHandler()
    plugins.find(_.applies(element)) match {
      case Some(plugin) => plugin.render(element, errorHandler)
      case None =>
        val ids = plugins.map(_.id)
        throw new Exception(s"Cannot serialize domain element '${element.id}' with plugins: $ids")
    }
  }
}
