package amf.core.internal.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}

case class DataNodePropertiesAnnotations(properties: Map[String, LexicalInformation]) extends SerializableAnnotation {

  /** Extension name. */
  override val name: String = "data-node-properties"

  /** Value as string. */
  override val value: String = properties
    .map { case (key, l) =>
      s"$key->${l.range}"
    }
    .mkString("#")
}

object DataNodePropertiesAnnotations extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] = {
    val tuples: Array[(String, LexicalInformation)] = value
      .split("#")
      .map(_.split("->") match {
        case Array(key, range) => key -> LexicalInformation(range)
      })
    Some(DataNodePropertiesAnnotations(tuples.toMap))
  }
}
