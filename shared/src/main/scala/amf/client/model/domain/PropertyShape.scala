package amf.client.model.domain

import amf.client.convert.CoreClientConverters._
import amf.client.model.{BoolField, IntField, StrField}
import amf.core.model.domain.extensions.{PropertyShape => InternalPropertyShape}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class PropertyShape(override private[amf] val _internal: InternalPropertyShape) extends Shape {

  @JSExportTopLevel("model.domain.PropertyShape")
  def this() = this(InternalPropertyShape())

  def path: StrField        = _internal.path
  def range: Shape          = _internal.range
  def minCount: IntField    = _internal.minCount
  def maxCount: IntField    = _internal.maxCount
  def patternName: StrField = _internal.patternName

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
