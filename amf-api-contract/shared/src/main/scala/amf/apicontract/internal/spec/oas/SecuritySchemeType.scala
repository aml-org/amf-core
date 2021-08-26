package amf.apicontract.internal.spec.oas

import amf.core.internal.remote.Spec

class SecuritySchemeType(val text: String)
case class OasSecuritySchemeType(override val text: String)     extends SecuritySchemeType(text)
case class UnknownSecuritySchemeType(override val text: String) extends SecuritySchemeType(text)

object OasLikeSecuritySchemeTypeMappings {

  private val OAuth20       = OasSecuritySchemeType("oauth2")
  private val ApiKeyOas     = OasSecuritySchemeType("apiKey")
  private val Http          = OasSecuritySchemeType("http")
  private val OpenIdConnect = OasSecuritySchemeType("openIdConnect")
  private val BasicAuth     = OasSecuritySchemeType("basic")

  val mappings = Map(
    Spec.OAS20 -> Oas2SchemeMappings,
    Spec.OAS30 -> Oas3SchemeMappings
  )

  def mapsTo(spec: Spec, text: String): SecuritySchemeType = mappings(spec).mapsTo(text)
  def validTypesFor(spec: Spec): Set[String]               = mappings(spec).validTypes

  abstract class SchemeMappings(val spec: Spec) {
    def applies(spec: Spec): Boolean = this.spec.id == spec.id
    def mapsTo(text: String): SecuritySchemeType
    def validTypes: Set[String] = types.keys.toSet
    def types: Map[String, OasSecuritySchemeType]
  }

  private object Oas2SchemeMappings extends SchemeMappings(Spec.OAS20) {

    lazy val types = Map(
      "OAuth 2.0"            -> OAuth20,
      "Basic Authentication" -> BasicAuth,
      "Api Key"              -> ApiKeyOas,
    )

    override def mapsTo(text: String): SecuritySchemeType = types.getOrElse(text, UnknownSecuritySchemeType(text))
  }

  private object Oas3SchemeMappings extends SchemeMappings(Spec.OAS30) {

    lazy val types = Map(
      "OAuth 2.0"             -> OAuth20,
      "Basic Authentication"  -> Http,
      "Digest Authentication" -> Http,
      "http"                  -> Http,
      "openIdConnect"         -> OpenIdConnect,
      "Api Key"               -> ApiKeyOas
    )

    override def mapsTo(text: String): SecuritySchemeType = types.getOrElse(text, UnknownSecuritySchemeType(text))
  }
}