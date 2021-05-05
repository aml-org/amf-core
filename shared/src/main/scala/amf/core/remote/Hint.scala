package amf.core.remote

import amf.core.parser.{ReferenceKind, UnspecifiedReference}
import amf.core.remote.Syntax.{Json, Syntax, Yaml}

case class Hint(vendor: Vendor, syntax: Syntax, kind: ReferenceKind = UnspecifiedReference) {
  def +(k: ReferenceKind): Hint = copy(kind = k)
}

object Raml10YamlHint extends Hint(Raml10, Yaml)

object Raml08YamlHint extends Hint(Raml08, Yaml)

object VocabularyYamlHint extends Hint(Aml, Yaml)

object VocabularyJsonHint extends Hint(Aml, Json)

object Oas20YamlHint extends Hint(Oas20, Yaml)

object Oas20JsonHint extends Hint(Oas20, Json)

object Oas30YamlHint extends Hint(Oas30, Yaml)

object Oas30JsonHint extends Hint(Oas30, Json)

object AsyncYamlHint extends Hint(AsyncApi, Yaml)

object AsyncJsonHint extends Hint(AsyncApi, Json)

object AmfJsonHint extends Hint(Amf, Json)

object PayloadJsonHint extends Hint(Payload, Json)

object PayloadYamlHint extends Hint(Payload, Yaml)
