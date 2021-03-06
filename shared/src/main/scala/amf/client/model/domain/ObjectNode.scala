package amf.client.model.domain

import amf.client.convert.CoreClientConverters._
import amf.core.model.domain.{ObjectNode => InternalObjectNode}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class ObjectNode(override private[amf] val _internal: InternalObjectNode) extends DataNode {

  @JSExportTopLevel("model.domain.ObjectNode")
  def this() = this(InternalObjectNode())

  def properties: ClientMap[DataNode] = _internal.allPropertiesWithName().asClient

  def getProperty(property: String): ClientOption[DataNode] = _internal.getFromKey(property).asClient

  def addProperty(property: String, node: DataNode): this.type = {
    _internal.addProperty(property, node._internal)
    this
  }
}
