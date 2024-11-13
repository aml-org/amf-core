package amf.core.internal.annotations

import amf.core.client.scala.model.domain._
import amf.core.internal.remote._

trait BaseSourceSpec extends SerializableAnnotation {
  val spec: Spec
  override val value: String = spec.id
}

case class DefinedBySpec(override val spec: Spec) extends BaseSourceSpec {
  override val name: String = "defined-by-spec"
}

object DefinedBySpec extends AnnotationGraphLoader {
  def parse(spec: String): Option[DefinedBySpec] = spec match {
    case Raml08.id     => Some(DefinedBySpec(Raml08))
    case Raml10.id     => Some(DefinedBySpec(Raml10))
    case Amf.id        => Some(DefinedBySpec(Amf))
    case Oas20.id      => Some(DefinedBySpec(Oas20))
    case Oas30.id      => Some(DefinedBySpec(Oas30))
    case Oas31.id      => Some(DefinedBySpec(Oas31))
    case AsyncApi.id   => Some(DefinedBySpec(AsyncApi))
    case JsonSchema.id => Some(DefinedBySpec(JsonSchema))
    case AvroSchema.id => Some(DefinedBySpec(AvroSchema))
    case _             => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    DefinedBySpec.parse(value)
}
