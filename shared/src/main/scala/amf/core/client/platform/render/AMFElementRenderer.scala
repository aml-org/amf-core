package amf.core.client.platform.render

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.parse.document.{ParsedDocument, SyamlParsedDocument}
import amf.core.client.scala.render.{AMFElementRenderer => InternalAMFElementRenderer}
import amf.core.internal.render.YNodeDocBuilderPopulator
import org.yaml.builder.DocBuilder

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AMFRenderer")
object AMFElementRenderer {

  def renderToBuilder[T](element: DomainElement, builder: DocBuilder[T], config: AMFGraphConfiguration): Unit = {
    val node =
      InternalAMFElementRenderer.renderElement(element, config).asInstanceOf[SyamlParsedDocument].document.node
    YNodeDocBuilderPopulator.populate(node, builder)
  }
}
