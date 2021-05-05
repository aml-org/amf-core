package amf.core.annotations

import amf.core.model.domain._
import amf.core.remote._

trait BaseSourceVendor extends SerializableAnnotation {
  val vendor: Vendor
  override val value: String = vendor.name
}

case class SourceVendor(override val vendor: Vendor) extends BaseSourceVendor with PerpetualAnnotation {
  override val name: String = "source-vendor"
}

object SourceVendor extends AnnotationGraphLoader {
  def parse(vendor: String): Option[SourceVendor] = vendor match {
    case Raml08.name => Some(SourceVendor(Raml08))
    case Raml10.name => Some(SourceVendor(Raml10))
    case Amf.name    => Some(SourceVendor(Amf))
    case Oas20.name  => Some(SourceVendor(Oas20))
    case Oas30.name  => Some(SourceVendor(Oas30))
    case _           => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    SourceVendor.parse(value)
}

case class DefinedByVendor(override val vendor: Vendor) extends BaseSourceVendor {
  override val name: String = "defined-by-vendor"
}

object DefinedByVendor extends AnnotationGraphLoader {
  def parse(vendor: String): Option[DefinedByVendor] = vendor match {
    case Raml08.name => Some(DefinedByVendor(Raml08))
    case Raml10.name => Some(DefinedByVendor(Raml10))
    case Amf.name    => Some(DefinedByVendor(Amf))
    case Oas20.name  => Some(DefinedByVendor(Oas20))
    case Oas30.name  => Some(DefinedByVendor(Oas30))
    case _           => None
  }

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    DefinedByVendor.parse(value)
}
