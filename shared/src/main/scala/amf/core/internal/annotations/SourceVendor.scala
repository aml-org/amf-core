package amf.core.internal.annotations

import amf.core.client.scala.model.domain._
import amf.core.internal.remote._

trait BaseSourceVendor extends SerializableAnnotation {
  val vendor: Spec
  override val value: String = vendor.id
}

case class SourceVendor(override val vendor: Spec) extends BaseSourceVendor with PerpetualAnnotation {
  override val name: String = "source-vendor"
}

object SourceVendor extends AnnotationGraphLoader {
  def parse(vendor: String): Option[SourceVendor] = vendor match {
    case Raml08.`id` => Some(SourceVendor(Raml08))
    case Raml10.`id` => Some(SourceVendor(Raml10))
    case Amf.`id`    => Some(SourceVendor(Amf))
    case Oas20.`id`  => Some(SourceVendor(Oas20))
    case Oas30.`id`  => Some(SourceVendor(Oas30))
    case _           => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    SourceVendor.parse(value)
}

case class DefinedByVendor(override val vendor: Spec) extends BaseSourceVendor {
  override val name: String = "defined-by-vendor"
}

object DefinedByVendor extends AnnotationGraphLoader {
  def parse(vendor: String): Option[DefinedByVendor] = vendor match {
    case Raml08.`id` => Some(DefinedByVendor(Raml08))
    case Raml10.`id` => Some(DefinedByVendor(Raml10))
    case Amf.`id`    => Some(DefinedByVendor(Amf))
    case Oas20.`id`  => Some(DefinedByVendor(Oas20))
    case Oas30.`id`  => Some(DefinedByVendor(Oas30))
    case _           => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    DefinedByVendor.parse(value)
}
