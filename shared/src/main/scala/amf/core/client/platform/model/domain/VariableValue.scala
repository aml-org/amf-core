package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.StrField
import amf.core.client.scala.model.domain.templates.{VariableValue => InternalVariableValue}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * VariableValue model class.
  */
@JSExportAll
case class VariableValue private[amf] (private[amf] val _internal: InternalVariableValue) extends DomainElement {

  @JSExportTopLevel("VariableValue")
  def this() = this(InternalVariableValue())

  def name: StrField  = _internal.name
  def value: DataNode = _internal.value

  def withName(name: String): this.type = {
    _internal.withName(name)
    this
  }

  def withValue(value: DataNode): this.type = {
    _internal.withValue(value._internal)
    this
  }
}
