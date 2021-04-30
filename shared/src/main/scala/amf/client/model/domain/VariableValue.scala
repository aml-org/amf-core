package amf.client.model.domain

import amf.client.convert.CoreClientConverters._
import amf.client.model.StrField
import amf.core.model.domain.templates.{VariableValue => InternalVariableValue}
import amf.core.parser.Annotations
import org.yaml.model.IllegalTypeHandler

import scala.scalajs.js.annotation.JSExportAll

/**
  * VariableValue model class.
  */
@JSExportAll
case class VariableValue private[amf] (private[amf] val _internal: InternalVariableValue) extends DomainElement {

  def this() = this(InternalVariableValue())

  def name: StrField  = _internal.name
  def value: DataNode = _internal.value

  def withName(name: String): this.type = {
    _internal.withName(name, Annotations())
    this
  }

  def withValue(value: DataNode): this.type = {
    _internal.withValue(value._internal)
    this
  }
}
