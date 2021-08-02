package amf.core.internal.annotations

import amf.core.client.scala.model.domain._
import amf.core.internal.remote._

trait BaseSourceSpec extends SerializableAnnotation {
  val spec: Spec
  override val value: String = spec.id
}

case class SourceSpec(override val spec: Spec) extends BaseSourceSpec with PerpetualAnnotation {
  override val name: String = "source-vendor"
}

object SourceSpec extends AnnotationGraphLoader {
  def parse(vendor: String): Option[SourceSpec] = vendor match {
    case Raml08.`id` => Some(SourceSpec(Raml08))
    case Raml10.`id` => Some(SourceSpec(Raml10))
    case Amf.`id`    => Some(SourceSpec(Amf))
    case Oas20.`id`  => Some(SourceSpec(Oas20))
    case Oas30.`id`  => Some(SourceSpec(Oas30))
    case _           => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    SourceSpec.parse(value)
}

case class DefinedBySpec(override val spec: Spec) extends BaseSourceSpec {
  override val name: String = "defined-by-spec"
}

object DefinedBySpec extends AnnotationGraphLoader {
  def parse(vendor: String): Option[DefinedBySpec] = vendor match {
    case Raml08.`id` => Some(DefinedBySpec(Raml08))
    case Raml10.`id` => Some(DefinedBySpec(Raml10))
    case Amf.`id`    => Some(DefinedBySpec(Amf))
    case Oas20.`id`  => Some(DefinedBySpec(Oas20))
    case Oas30.`id`  => Some(DefinedBySpec(Oas30))
    case _           => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    DefinedBySpec.parse(value)
}
