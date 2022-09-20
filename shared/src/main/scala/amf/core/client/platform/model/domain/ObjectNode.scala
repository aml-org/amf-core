package amf.core.client.platform.model.domain

import amf.core.client.platform.model.domain.common.DescribedElement
import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.domain.{ObjectNode => InternalObjectNode}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class ObjectNode(override private[amf] val _internal: InternalObjectNode) extends DataNode with DescribedElement {

  @JSExportTopLevel("ObjectNode")
  def this() = this(InternalObjectNode())

  def properties: ClientMap[DataNode] = _internal.allPropertiesWithName().asClient

  def getProperty(property: String): ClientOption[DataNode] = _internal.getFromKey(property).asClient

  def addProperty(property: String, node: DataNode): this.type = {
    _internal.addProperty(property, node._internal)
    this
  }
}
