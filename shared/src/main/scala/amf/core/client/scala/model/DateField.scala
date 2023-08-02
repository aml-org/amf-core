package amf.core.client.scala.model

import org.mulesoft.common.time.SimpleDateTime

trait DateField extends ValueField[SimpleDateTime] {

  /** Return value or null. */
  override def value(): SimpleDateTime = option().orNull
}
