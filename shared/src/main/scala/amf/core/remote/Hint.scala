package amf.core.remote

import amf.core.parser.{ReferenceKind, UnspecifiedReference}
import amf.core.remote.Syntax.{Json, Syntax, Yaml}

case class Hint(vendor: Vendor, syntax: Syntax, kind: ReferenceKind = UnspecifiedReference) {
  def +(k: ReferenceKind): Hint = copy(kind = k)
}

object RamlYamlHint extends Hint(Raml, Yaml)

object VocabularyYamlHint extends Hint(Aml, Yaml)

object VocabularyJsonHint extends Hint(Aml, Json)

object RamlJsonHint extends Hint(Raml, Json)

object OasYamlHint extends Hint(Oas, Yaml)

object OasJsonHint extends Hint(Oas, Json)

object AsyncYamlHint extends Hint(AsyncApi, Yaml)

object AsyncJsonHint extends Hint(AsyncApi, Json)

object AmfJsonHint extends Hint(Amf, Json)

object PayloadJsonHint extends Hint(Payload, Json)

object PayloadYamlHint extends Hint(Payload, Yaml)