package amf.core.client.platform.model

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.{FloatField => InternalFloatField}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class FloatField(private val _internal: InternalFloatField) extends ValueField[Float] {

  override protected val _option: Option[Float] = _internal.option()

  /** Return value as option. */
  override val option: ClientOption[Float] = _option.asClient

  /** Return annotations. */
  override def annotations(): Annotations = _internal.annotations()

  /** Return float value or `0.0f` if value is null or undefined. */
  override def value(): Float = _option match {
    case Some(v) => v
    case _       => 0.0f
  }

  override def remove(): Unit = _internal.remove()
}
