package amf.core.internal.metamodel

import amf.core.internal.metamodel.domain.ModelDoc
import amf.core.client.scala.vocabulary.ValueType

/** Field
  */
case class Field(
    `type`: Type,
    value: ValueType,
    doc: ModelDoc = ModelDoc(),
    jsonldField: Boolean = true,
    deprecated: Boolean = false,
    defaultValue: Option[Any] = None
) {
  override def toString: String = value.iri()

  override def canEqual(a: Any): Boolean = Option(a).isDefined && a.isInstanceOf[Field]

  override def equals(that: Any): Boolean =
    that match {
      case that: Field => that.value.iri() == value.iri()
      case _           => false
    }

}
