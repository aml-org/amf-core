package amf.core.internal.remote

import amf.core.internal.remote.Mimes._

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

@JSExportTopLevel("Spec")
object Spec {
  def unapply(name: String): Option[Spec] = {
    name match {
      case Raml10.id            => Some(Raml10)
      case Raml08.id            => Some(Raml08)
      case Oas20.id             => Some(Oas20)
      case Oas30.id             => Some(Oas30)
      case AwsOas30.id          => Some(AwsOas30)
      case AsyncApi20.id        => Some(AsyncApi20)
      case AsyncApi21.id        => Some(AsyncApi21)
      case AsyncApi22.id        => Some(AsyncApi22)
      case AsyncApi23.id        => Some(AsyncApi23)
      case AsyncApi24.id        => Some(AsyncApi24)
      case AsyncApi25.id        => Some(AsyncApi25)
      case AsyncApi26.id        => Some(AsyncApi26)
      case Amf.id               => Some(Amf)
      case Payload.id           => Some(Payload)
      case Aml.id               => Some(Aml)
      case JsonSchema.id        => Some(JsonSchema)
      case Grpc.id              => Some(Grpc)
      case GraphQL.id           => Some(GraphQL)
      case GraphQLFederation.id => Some(GraphQLFederation)
      case JsonSchemaDialect.id => Some(JsonSchemaDialect)
      case JsonLDSchema.id      => Some(JsonLDSchema)
      case AvroSchema.id        => Some(AvroSchema)
      case _                    => None
    }
  }

  @JSExport("apply")
  def apply(name: String): Spec = name match {
    case Spec(spec) => spec
    case _          => UnknownSpec(name)
  }

  @JSExport val RAML08: Spec             = Raml08
  @JSExport val RAML10: Spec             = Raml10
  @JSExport val OAS20: Spec              = Oas20
  @JSExport val OAS30: Spec              = Oas30
  @JSExport val AWS_OAS30: Spec          = AwsOas30
  @JSExport val ASYNC20: Spec            = AsyncApi20
  @JSExport val ASYNC21: Spec            = AsyncApi21
  @JSExport val ASYNC22: Spec            = AsyncApi22
  @JSExport val ASYNC23: Spec            = AsyncApi23
  @JSExport val ASYNC24: Spec            = AsyncApi24
  @JSExport val ASYNC25: Spec            = AsyncApi25
  @JSExport val ASYNC26: Spec            = AsyncApi26
  @JSExport val AMF: Spec                = Amf
  @JSExport val PAYLOAD: Spec            = Payload
  @JSExport val AML: Spec                = Aml
  @JSExport val JSONSCHEMA: Spec         = JsonSchema
  @JSExport val AVRO_SCHEMA: Spec        = AvroSchema
  @JSExport val GRPC: Spec               = Grpc
  @JSExport val GRAPHQL: Spec            = GraphQL
  @JSExport val GRAPHQL_FEDERATION: Spec = GraphQLFederation
  @JSExport val JSONSCHEMADIALECT: Spec  = JsonSchemaDialect
  @JSExport val JSONDLSCHEMA: Spec       = JsonLDSchema
}

@JSExportAll
trait Spec {
  val id: String

  def isRaml: Boolean = this == Raml10 || this == Raml08
  def isOas: Boolean  = this == Oas20 || this == Oas30
  def isAsync: Boolean =
    this == AsyncApi || this == AsyncApi20 || this == AsyncApi21 || this == AsyncApi22 || this == AsyncApi23 || this == AsyncApi24 || this == AsyncApi25 || this == AsyncApi26

  val mediaType: String
}

case class UnknownSpec(override val id: String) extends Spec {
  override val mediaType: String = "application/unknown"
}

case class AmlDialectSpec(override val id: String) extends Spec {
  override val mediaType: String = `application/yaml`
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

  override val mediaType: String = `application/yaml`
}

private[amf] case object Oas20 extends Oas {
  override def version: String = "2.0"

  override val mediaType: String = `application/json`
}

private[amf] case object Oas30 extends Oas {
  override def version: String = "3.0"

  override val mediaType: String = `application/yaml`
}

private[amf] case object AwsOas30 extends Oas {
  override def version: String = "3.0"

  override val id: String = ("AWS OAS " + version).trim

  override val mediaType: String = `application/yaml`
}

private[amf] case object Raml08 extends Raml {
  override def version: String   = "0.8"
  override val mediaType: String = `application/yaml`
}

private[amf] case object Raml10 extends Raml {
  override def version: String = "1.0"

  override val mediaType: String = `application/yaml`
}

private[amf] case object AsyncApi extends Async {
  override def version: String = ""

  override val mediaType: String = `application/yaml`
}

private[amf] case object AsyncApi20 extends Async {
  override def version: String = "2.0"

  override val mediaType: String = `application/yaml`

}

private[amf] case object AsyncApi21 extends Async {
  override def version: String = "2.1"

  override val mediaType: String = `application/yaml`

}

private[amf] case object AsyncApi22 extends Async {
  override def version: String = "2.2"

  override val mediaType: String = `application/yaml`

}

private[amf] case object AsyncApi23 extends Async {
  override def version: String = "2.3"

  override val mediaType: String = `application/yaml`

}

private[amf] case object AsyncApi24 extends Async {
  override def version: String = "2.4"

  override val mediaType: String = `application/yaml`

}

private[amf] case object AsyncApi25 extends Async {
  override def version: String = "2.5"

  override val mediaType: String = `application/yaml`

}

private[amf] case object AsyncApi26 extends Async {
  override def version: String = "2.6"

  override val mediaType: String = `application/yaml`

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

  override val mediaType: String = `application/json`

  override def toString: String = id.trim
}

// keep private?
private[amf] case object JSONRefs extends Spec {
  override val id: String        = "JSON + Refs"
  override val mediaType: String = `application/json`
}

private[amf] case object Grpc extends Spec {
  override val id: String        = "Grpc"
  override val mediaType: String = `application/grpc`
}

private[amf] case object GraphQL extends Spec {
  override val id: String        = "GraphQL"
  override val mediaType: String = `application/graphql`
}

private[amf] case object GraphQLFederation extends Spec {
  override val id: String        = "GraphQLFederation"
  override val mediaType: String = `application/graphql`
}

private[amf] case object JsonSchemaDialect extends Spec {
  override val id: String        = "JSON Schema Dialect"
  override val mediaType: String = `application/semantics+schema+json`
}

private[amf] case object JsonLDSchema extends Spec {
  override val id: String        = "JSONLD Schema"
  override val mediaType: String = `application/schema+ld+json`
}

private[amf] case object AvroSchema extends Spec {
  override val id: String        = "Avro"
  override val mediaType: String = `application/json`
}
