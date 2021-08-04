package amf.core.client.scala.model

trait FloatField extends ValueField[Float] {

  /** Return float value or `0.0f` if value is null or undefined. */
  override def value(): Float = option() match {
    case Some(v) => v
    case _       => 0.0f
  }
}
