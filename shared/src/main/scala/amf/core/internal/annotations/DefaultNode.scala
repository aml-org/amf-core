package amf.core.internal.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, PerpetualAnnotation, SerializableAnnotation}

/** Node generated during parsing that should not be taken into consideration for resolution
  */
case class DefaultNode() extends SerializableAnnotation with PerpetualAnnotation {
  override val name: String  = "default-node"
  override val value: String = ""
}

object DefaultNode extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] = Some(DefaultNode())
  def name: String = "default-node"
}
