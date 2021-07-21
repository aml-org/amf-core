package amf.core.internal.remote

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

@JSExportTopLevel("Vendor")
object Vendor {
  def unapply(name: String): Option[Vendor] = {
    name match {
      case Raml10.name     => Some(Raml10)
      case Raml08.name     => Some(Raml08)
      case Oas20.name      => Some(Oas20)
      case Oas30.name      => Some(Oas30)
      case AsyncApi20.name => Some(AsyncApi20)
      case Amf.name        => Some(Amf)
      case Payload.name    => Some(Payload)
      case Aml.name        => Some(Aml)
      case JsonSchema.name => Some(JsonSchema)
      case _               => None
    }
  }

  @JSExport("apply")
  def apply(name: String): Vendor = name match {
    case Vendor(vendor) => vendor
    case _              => new UnknownVendor(name)
  }

  @JSExport val RAML08: Vendor     = Raml08
  @JSExport val RAML10: Vendor     = Raml10
  @JSExport val OAS20: Vendor      = Oas20
  @JSExport val OAS30: Vendor      = Oas30
  @JSExport val ASYNC20: Vendor    = AsyncApi20
  @JSExport val AMF: Vendor        = Amf
  @JSExport val PAYLOAD: Vendor    = Payload
  @JSExport val AML: Vendor        = Aml
  @JSExport val JSONSCHEMA: Vendor = JsonSchema
}

@JSExportAll
trait Vendor {
  val name: String

  def isRaml: Boolean  = this == Raml10 || this == Raml08
  def isOas: Boolean   = this == Oas20 || this == Oas30
  def isAsync: Boolean = this == AsyncApi || this == AsyncApi20

  val mediaType: String
}

class UnknownVendor(override val name: String) extends Vendor {
  override val mediaType: String = "application/unknown"
}

trait Raml extends Vendor {
  def version: String

  override val name: String = ("RAML " + version).trim

  override def toString: String = name.trim
}

trait Oas extends Vendor {
  def version: String

  override val name: String = ("OAS " + version).trim

  override def toString: String = name.trim
}

trait Async extends Vendor {
  def version: String

  override val name: String = ("ASYNC " + version).trim

  override def toString: String = name.trim
}

case object Aml extends Vendor {

  override val name: String = "AML 1.0"

  override def toString: String = name.trim

  override val mediaType: String = "application/aml"
}

case object Oas20 extends Oas {
  override def version: String = "2.0"

  override val mediaType: String = "application/json"
}

case object Oas30 extends Oas {
  override def version: String = "3.0"

  override val mediaType: String = "application/json"
}

case object Raml08 extends Raml {
  override def version: String   = "0.8"
  override val mediaType: String = "application/yaml"
}

case object Raml10 extends Raml {
  override def version: String = "1.0"

  override val mediaType: String = "application/raml10"
}

case object AsyncApi extends Async {
  override def version: String = ""

  override val mediaType: String = "application/yaml"
}

case object AsyncApi20 extends Async {
  override def version: String = "2.0"

  override val mediaType: String = "application/yaml"

}

case object Amf extends Vendor {
  override val name: String = "AMF Graph"

  override val mediaType: String = "application/ld+json"

}

case object Payload extends Vendor {
  override val name: String = "AMF Payload"

  override val mediaType: String = "application/amf-payload"
}

case object JsonSchema extends Vendor {
  override val name: String = "JSON Schema"

  override val mediaType: String = "application/json"

  override def toString: String = name.trim
}
