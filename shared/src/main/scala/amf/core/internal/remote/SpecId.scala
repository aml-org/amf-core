package amf.core.internal.remote

import amf.core.internal.remote.Mimes._

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

@JSExportTopLevel("SpecId")
object SpecId {
  def unapply(name: String): Option[SpecId] = {
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
  def apply(name: String): SpecId = name match {
    case SpecId(vendor) => vendor
    case _              => UnknownSpecId(name)
  }

  @JSExport val RAML08: SpecId     = Raml08
  @JSExport val RAML10: SpecId     = Raml10
  @JSExport val OAS20: SpecId      = Oas20
  @JSExport val OAS30: SpecId      = Oas30
  @JSExport val ASYNC20: SpecId    = AsyncApi20
  @JSExport val AMF: SpecId        = Amf
  @JSExport val PAYLOAD: SpecId    = Payload
  @JSExport val AML: SpecId        = Aml
  @JSExport val JSONSCHEMA: SpecId = JsonSchema
}

@JSExportAll
trait SpecId {
  val name: String

  def isRaml: Boolean  = this == Raml10 || this == Raml08
  def isOas: Boolean   = this == Oas20 || this == Oas30
  def isAsync: Boolean = this == AsyncApi || this == AsyncApi20

  val mediaType: String
}

case class UnknownSpecId(override val name: String) extends SpecId {
  override val mediaType: String = "application/unknown"
}

private[amf] trait Raml extends SpecId {
  def version: String

  override val name: String = ("RAML " + version).trim

  override def toString: String = name.trim
}

private[amf] trait Oas extends SpecId {
  def version: String

  override val name: String = ("OAS " + version).trim

  override def toString: String = name.trim
}

private[amf] trait Async extends SpecId {
  def version: String

  override val name: String = ("ASYNC " + version).trim

  override def toString: String = name.trim
}

private[amf] case object Aml extends SpecId {

  override val name: String = "AML 1.0"

  override def toString: String = name.trim

  override val mediaType: String = "application/aml"
}

private[amf] case object Oas20 extends Oas {
  override def version: String = "2.0"

  override val mediaType: String = "application/oas20"
}

private[amf] case object Oas30 extends Oas {
  override def version: String = "3.0"

  override val mediaType: String = "application/openapi30"
}

private[amf] case object Raml08 extends Raml {
  override def version: String   = "0.8"
  override val mediaType: String = "application/raml08"
}

private[amf] case object Raml10 extends Raml {
  override def version: String = "1.0"

  override val mediaType: String = "application/raml10"
}

private[amf] case object AsyncApi extends Async {
  override def version: String = ""

  override val mediaType: String = `application/asyncapi`
}

private[amf] case object AsyncApi20 extends Async {
  override def version: String = "2.0"

  override val mediaType: String = "application/asyncapi20"

}

private[amf] case object Amf extends SpecId {
  override val name: String = "AMF Graph"

  override val mediaType: String = `application/ld+json`

}

private[amf] case object Payload extends SpecId {
  override val name: String = "AMF Payload"

  override val mediaType: String = "application/amf-payload"
}

private[amf] case object JsonSchema extends SpecId {
  override val name: String = "JSON Schema"

  override val mediaType: String = "application/schema+json"

  override def toString: String = name.trim
}
