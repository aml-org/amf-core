package amf.core.internal.remote

import amf.core.client.scala.parse.document.{ReferenceKind, UnspecifiedReference}
import amf.core.internal.remote.Syntax.{Json, JsonLd, Protobuf, Syntax, Yaml, GraphQL => GraphQLSyntax}

case class Hint(spec: Spec, syntax: Syntax, kind: ReferenceKind = UnspecifiedReference) {
  def +(k: ReferenceKind): Hint = copy(kind = k)

}

object GraphQLHint extends Hint(GraphQL, GraphQLSyntax)

object GraphQLFederationHint extends Hint(GraphQLFederation, GraphQLSyntax)

object GrpcProtoHint extends Hint(Grpc, Protobuf)

object AmlHint extends Hint(Aml, Yaml)

object Raml10YamlHint extends Hint(Raml10, Yaml)

object Raml08YamlHint extends Hint(Raml08, Yaml)

object VocabularyYamlHint extends Hint(Aml, Yaml)

object VocabularyJsonHint extends Hint(Aml, Json)

object Oas20YamlHint extends Hint(Oas20, Yaml)

object Oas20JsonHint extends Hint(Oas20, Json)

object Oas30YamlHint extends Hint(Oas30, Yaml)

object Oas31YamlHint extends Hint(Oas31, Yaml)

object Oas30JsonHint extends Hint(Oas30, Json)

object Oas31JsonHint extends Hint(Oas31, Json)

object Async20YamlHint extends Hint(AsyncApi20, Yaml)

object Async20JsonHint extends Hint(AsyncApi20, Json)

object AmfJsonHint extends Hint(Amf, JsonLd)

object PayloadJsonHint extends Hint(Payload, Json)

object PayloadYamlHint extends Hint(Payload, Yaml)

object JsonSchemaHint extends Hint(JsonSchema, Json)

object AvroHint extends Hint(AvroSchema, Json)
