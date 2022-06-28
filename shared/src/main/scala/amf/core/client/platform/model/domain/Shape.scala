package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.{BoolField, StrField}
import amf.core.client.scala.model.domain.{Shape => InternalShape}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait Shape extends DomainElement with Linkable with NamedDomainElement {

  override private[amf] val _internal: InternalShape

  def name: StrField            = _internal.name
  def displayName: StrField     = _internal.displayName
  def description: StrField     = _internal.description
  def defaultValue: DataNode    = _internal.default
  def defaultValueStr: StrField = _internal.defaultString

  def values: ClientList[DataNode]                              = _internal.values.asClient
  def inherits: ClientList[Shape]                               = _internal.inherits.asClient
  def customShapeProperties: ClientList[ShapeExtension]         = _internal.customShapeProperties.asClient
  def customShapePropertyDefinitions: ClientList[PropertyShape] = _internal.customShapePropertyDefinitions.asClient
  def or: ClientList[Shape]                                     = _internal.or.asClient
  def and: ClientList[Shape]                                    = _internal.and.asClient
  def xone: ClientList[Shape]                                   = _internal.xone.asClient
  def not: Shape                                                = _internal.not
  def readOnly: BoolField                                       = _internal.readOnly
  def writeOnly: BoolField                                      = _internal.writeOnly
  def deprecated: BoolField                                     = _internal.deprecated
  def ifShape: Shape                                            = _internal.ifShape
  def elseShape: Shape                                          = _internal.elseShape
  def thenShape: Shape                                          = _internal.thenShape
  def isExtension: BoolField                                    = _internal.isExtension

  def withName(name: String): this.type = {
    _internal.withName(name)
    this
  }

  def withDisplayName(name: String): this.type = {
    _internal.withDisplayName(name)
    this
  }

  def withDescription(description: String): this.type = {
    _internal.withDescription(description)
    this
  }

  def withDefaultValue(defaultVal: DataNode): this.type = {
    _internal.withDefault(defaultVal)
    this
  }

  def withValues(values: ClientList[DataNode]): this.type = {
    _internal.withValues(values.asInternal)
    this
  }

  def withInherits(inherits: ClientList[Shape]): this.type = {
    _internal.withInherits(inherits.asInternal)
    this
  }

  def withOr(subShapes: ClientList[Shape]): this.type = {
    _internal.withOr(subShapes.asInternal)
    this
  }

  def withAnd(subShapes: ClientList[Shape]): this.type = {
    _internal.withAnd(subShapes.asInternal)
    this
  }

  def withXone(subShapes: ClientList[Shape]): this.type = {
    _internal.withXone(subShapes.asInternal)
    this
  }

  def withNode(shape: Shape): this.type = {
    _internal.withNot(shape)
    this
  }

  def withDefaultStr(value: String): this.type = {
    _internal.withDefaultStr(value)
    this
  }

  def withCustomShapeProperties(customShapeProperties: ClientList[ShapeExtension]): this.type = {
    _internal.withCustomShapeProperties(customShapeProperties.asInternal)
    this
  }

  def withCustomShapePropertyDefinitions(propertyDefinitions: ClientList[PropertyShape]): this.type = {
    _internal.withCustomShapePropertyDefinitions(propertyDefinitions.asInternal)
    this
  }

  def withCustomShapePropertyDefinition(name: String): PropertyShape = {
    _internal.withCustomShapePropertyDefinition(name)
  }

  def withReadOnly(readOnly: Boolean): this.type = {
    _internal.withReadOnly(readOnly)
    this
  }

  def withWriteOnly(writeOnly: Boolean): this.type = {
    _internal.withWriteOnly(writeOnly)
    this
  }

  def withDeprecated(deprecated: Boolean): this.type = {
    _internal.withDeprecated(deprecated)
    this
  }

  def withIf(ifShape: Shape): this.type = {
    _internal.withIf(ifShape)
    this
  }

  def withElse(elseShape: Shape): this.type = {
    _internal.withElse(elseShape)
    this
  }

  def withThen(thenShape: Shape): this.type = {
    _internal.withThen(thenShape)
    this
  }

  def withIsExtension(value: Boolean): this.type = {
    _internal.withIsExtension(value)
    this
  }

  def hasExplicitName: Boolean = _internal.hasExplicitName
}
