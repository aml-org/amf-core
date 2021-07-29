package amf.core.internal.remote

import amf.core.internal.remote.Mimes._

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

@JSExportTopLevel("SpecId")
object Spec {
  def unapply(name: String): Option[Spec] = {
    name match {
      case Raml10.`id`     => Some(Raml10)
      case Raml08.`id`     => Some(Raml08)
      case Oas20.`id`      => Some(Oas20)
      case Oas30.`id`      => Some(Oas30)
      case AsyncApi20.`id` => Some(AsyncApi20)
      case Amf.`id`        => Some(Amf)
      case Payload.`id`    => Some(Payload)
      case Aml.`id`        => Some(Aml)
      case JsonSchema.`id` => Some(JsonSchema)
      case _               => None
    }
  }

  @JSExport("apply")
  def apply(name: String): Spec = name match {
    case Spec(vendor) => vendor
    case _            => UnknownSpec(name)
  }

  @JSExport val RAML08: Spec     = Raml08
  @JSExport val RAML10: Spec     = Raml10
  @JSExport val OAS20: Spec      = Oas20
  @JSExport val OAS30: Spec      = Oas30
  @JSExport val ASYNC20: Spec    = AsyncApi20
  @JSExport val AMF: Spec        = Amf
  @JSExport val PAYLOAD: Spec    = Payload
  @JSExport val AML: Spec        = Aml
  @JSExport val JSONSCHEMA: Spec = JsonSchema
}

@JSExportAll
trait Spec {
  val id: String

  def isRaml: Boolean  = this == Raml10 || this == Raml08
  def isOas: Boolean   = this == Oas20 || this == Oas30
  def isAsync: Boolean = this == AsyncApi || this == AsyncApi20

  val mediaType: String
}

case class UnknownSpec(override val id: String) extends Spec {
  override val mediaType: String = "application/unknown"
}

case class AmlDialectSpec(override val id: String) extends Spec {
  override val mediaType: String = "application/aml"
}

private[amf] trait Raml extends Spec {
  def version: String

  override val id: String = ("RAML " + version).trim

  override def toString: String = id.trim
}

private[amf] trait Oas extends Spec {
  def version: String

  override val id: String = ("OAS " + version).trim

  override def toString: String = id.trim
}

private[amf] trait Async extends Spec {
  def version: String

  override val id: String = ("ASYNC " + version).trim

  override def toString: String = id.trim
}

private[amf] case object Aml extends Spec {

  override val id: String = "AML 1.0"

  override def toString: String = id.trim

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

private[amf] case object Amf extends Spec {
  override val id: String = "AMF Graph"

  override val mediaType: String = `application/ld+json`

}

private[amf] case object Payload extends Spec {
  override val id: String = "AMF Payload"

  override val mediaType: String = "application/amf-payload"
}

private[amf] case object JsonSchema extends Spec {
  override val id: String = "JSON Schema"

  override val mediaType: String = "application/schema+json"

  override def toString: String = id.trim
}
