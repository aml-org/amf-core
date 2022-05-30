package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.StrField
import amf.core.client.scala.model.domain.templates.{ParametrizedDeclaration => InternalParametrizedDeclaration}

import scala.scalajs.js.annotation.JSExportAll

/** ParametrizedDeclaration model class.
  */
@JSExportAll
trait ParametrizedDeclaration extends DomainElement with NamedDomainElement {

  override private[amf] val _internal: InternalParametrizedDeclaration

  def name: StrField                       = _internal.name
  def target: AbstractDeclaration          = _internal.target
  def variables: ClientList[VariableValue] = _internal.variables.asClient

  /** Set name property. */
  def withName(name: String): this.type = {
    _internal.withName(name)
    this
  }

  /** Set the target property. */
  def withTarget(target: AbstractDeclaration): this.type = {
    _internal.withTarget(target)
    this
  }

  /** Set variables property. */
  def withVariables(variables: ClientList[VariableValue]): this.type = {
    _internal.withVariables(variables.asInternal)
    this
  }
}
