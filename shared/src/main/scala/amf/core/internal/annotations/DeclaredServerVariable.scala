package amf.core.internal.annotations

import amf.core.client.scala.model.domain._

/*
  This annotation is used in Async 2.4 to differentiate parameter declarations from Server Variable declarations
 */
case class DeclaredServerVariable() extends SerializableAnnotation with PerpetualAnnotation {
  override val name: String  = "declared-server-variable"
  override val value: String = ""
}

object DeclaredServerVariable extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(DeclaredServerVariable())
}
