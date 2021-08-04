package amf.core.client.platform.model

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.{DoubleField => InternalDoubleField}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class DoubleField(private val _internal: InternalDoubleField) extends ValueField[Double] {

  override protected val _option: Option[Double] = _internal.option()

  /** Return value as option. */
  override val option: ClientOption[Double] = _option.asClient

  /** Return annotations. */
  override def annotations(): Annotations = _internal.annotations()

  /** Return double value or `0.0` if value is null or undefined. */
  override def value(): Double = _option match {
    case Some(v) => v
    case _       => 0.0
  }

  override def remove(): Unit = _internal.remove()
}
