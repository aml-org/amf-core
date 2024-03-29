package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.StrField
import amf.core.client.scala.model.domain.{DataNode => InternalDataNode}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait DataNode extends DomainElement {

  override private[amf] val _internal: InternalDataNode

  def name: StrField = _internal.name

  def withName(name: String): this.type = {
    _internal.withName(name)
    this
  }
}
