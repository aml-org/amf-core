package amf.core.client.scala.model.domain

trait Annotation

trait PerpetualAnnotation extends Annotation

trait SerializableAnnotation extends Annotation {

  /** Extension name. */
  val name: String

  /** Value as string. */
  def value: String
}

trait ResolvableAnnotation extends Annotation {

  /** To allow deferred resolution on unordered graph parsing. */
  def resolve(objects: Map[String, AmfElement]): Unit = {}
}

trait AnnotationGraphLoader {
  def unparse(annotatedValue: String, objects: Map[String, AmfElement]): Option[Annotation]
}

trait UriAnnotation {
  def uris: Seq[String]
  def shorten(fn: String => String): Annotation
}

trait EternalSerializedAnnotation extends PerpetualAnnotation with SerializableAnnotation
