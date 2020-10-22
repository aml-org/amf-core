package amf.core.annotations

import amf.core.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}

trait VirtualNode extends Annotation

case class SynthesizedField() extends SerializableAnnotation with VirtualNode {
  override val name: String  = "synthesized-field"
  override val value: String = "true"
}

object SynthesizedField extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(SynthesizedField())
}

case class Inferred() extends VirtualNode

case class VirtualObject() extends VirtualNode
