package amf.client.model.domain

import amf.client.convert.CoreClientConverters._
import amf.client.model.StrField
import amf.core.model.domain.{RecursiveShape => InternalRecursiveShape}
import amf.core.unsafe.PlatformSecrets

import scala.scalajs.js.annotation.JSExport

case class RecursiveShape(private[amf] override val _internal: InternalRecursiveShape)
    extends Shape
    with PlatformSecrets {

  @JSExport
  def fixpoint: StrField = _internal.fixpoint

  @JSExport
  def withFixPoint(shapeId: String): this.type = {
    _internal.withFixPoint(shapeId)
    this
  }

  @JSExport
  override def linkCopy(): Linkable = throw new Exception("Recursive shape cannot be linked")
}
