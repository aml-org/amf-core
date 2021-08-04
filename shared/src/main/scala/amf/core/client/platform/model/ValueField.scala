package amf.core.client.platform.model

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.{ValueField => InternalValueField}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait ValueField[T] extends Annotable {

  protected val _option: Option[T]

  /** Return value as option. */
  val option: ClientOption[T]

  /** Return value or null. */
  def value(): T

  /** Return true if string value is equals to non-null given value. */
  def is(other: T): Boolean = _option.fold(false)(_ == other)

  /** Return true if string value is not-null and accepted by given function. */
  def is(accepts: T => Boolean): Boolean = _option.fold(false)(accepts(_))

  /** Returns true if field is null. */
  def isNull: Boolean = _option.isEmpty

  /** Returns true if field is non null. */
  def nonNull: Boolean = _option.isDefined

  override def toString: String = _option.map(_.toString).orNull

  def remove(): Unit
}
