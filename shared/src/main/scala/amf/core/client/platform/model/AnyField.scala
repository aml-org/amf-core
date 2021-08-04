package amf.core.client.platform.model

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.{AnyField => InternalAnyField}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class AnyField(private val _internal: InternalAnyField) extends ValueField[Any] {

  override protected val _option: Option[Any] = _internal.option()

  /** Return value as option. */
  override val option: ClientOption[Any] = _option.asClient

  /** Return annotations. */
  override def annotations(): Annotations = _internal.annotations()

  /** Return value or null if value is null or undefined. */
  override def value(): Any = _option.orNull

  override def remove(): Unit = _internal.remove()
}
