package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.{BoolField, IntField, StrField}
import amf.core.client.scala.model.domain.extensions.{PropertyShape => InternalPropertyShape}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class PropertyShape(override private[amf] val _internal: InternalPropertyShape) extends Shape {

  @JSExportTopLevel("PropertyShape")
  def this() = this(InternalPropertyShape())

  def path: StrField               = _internal.path
  def range: Shape                 = _internal.range
  def minCount: IntField           = _internal.minCount
  def maxCount: IntField           = _internal.maxCount
  def patternName: StrField        = _internal.patternName
  def serializationOrder: IntField = _internal.serializationOrder

  def withSerializationOrder(order: Int): this.type = {
    _internal.withSerializationOrder(order)
    this
  }

  def withPath(path: String): this.type = {
    _internal.withPath(path)
    this
  }

  def withRange(range: Shape): this.type = {
    _internal.withRange(range._internal)
    this
  }

  def withMinCount(min: Int): this.type = {
    _internal.withMinCount(min)
    this
  }
  def withMaxCount(max: Int): this.type = {
    _internal.withMaxCount(max)
    this
  }

  def withPatternName(pattern: String): this.type = {
    _internal.withPatternName(pattern)
    this
  }

  override def linkCopy(): PropertyShape = _internal.linkCopy()
}
