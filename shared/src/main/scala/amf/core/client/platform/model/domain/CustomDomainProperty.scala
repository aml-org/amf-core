package amf.core.client.platform.model.domain

import amf.core.client.platform.model.{StrField, IntField}
import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty => InternalCustomDomainProperty}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class CustomDomainProperty(private[amf] val _internal: InternalCustomDomainProperty)
    extends DomainElement
    with Linkable {

  @JSExportTopLevel("CustomDomainProperty")
  def this() = this(InternalCustomDomainProperty())

  def name: StrField = _internal.name

  def displayName: StrField = _internal.displayName

  def description: StrField = _internal.description

  def domain: ClientList[StrField] = _internal.domain.asClient

  def schema: Shape = _internal.schema
  def serializationOrder: IntField = _internal.serializationOrder

  def withSerializationOrder(order: Int): this.type = {
    _internal.withSerializationOrder(order)
    this
  }

  def withName(name: String): this.type = {
    _internal.withName(name)
    this
  }

  def withDisplayName(displayName: String): this.type = {
    _internal.withDisplayName(displayName)
    this
  }

  def withDescription(description: String): this.type = {
    _internal.withDescription(description)
    this
  }

  def withDomain(domain: ClientList[String]): this.type = {
    _internal.withDomain(domain.asInternal)
    this
  }

  def withSchema(schema: Shape): this.type = {
    _internal.withSchema(schema)
    this
  }

  override def linkCopy(): CustomDomainProperty = _internal.linkCopy()
}
