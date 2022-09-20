package amf.core.client.platform.model.domain

import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.domain.common.DescribedElement
import amf.core.client.scala.model.domain.{ScalarNode => InternalScalarNode}
import amf.core.internal.convert.CoreClientConverters._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class ScalarNode(override private[amf] val _internal: InternalScalarNode) extends DataNode with DescribedElement {

  @JSExportTopLevel("ScalarNode")
  def this() = this(InternalScalarNode())

  @JSExportTopLevel("ScalarNode")
  def this(value: String, dataType: String) = this(InternalScalarNode(value, Option(dataType)))

  def value: StrField    = _internal.value
  def dataType: StrField = _internal.dataType

  override def toString = s"${name.value()}:$dataType=$value"

  /** Set name property of this value. */
  def withValue(value: String): this.type = {
    _internal.withValue(value)
    this
  }

  /** Set name property of this dataType. */
  def withDataType(dataType: String): this.type = {
    _internal.withDataType(dataType)
    this
  }
}

object ScalarNode {
  def build(value: String, dataType: String) = new ScalarNode(value, dataType)
}
