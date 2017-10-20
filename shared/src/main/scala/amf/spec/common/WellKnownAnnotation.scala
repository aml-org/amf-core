package amf.spec.common

object WellKnownAnnotation {

  val annotations: Map[Object, Boolean] = Map(
    "(termsOfService)"      -> true,
    "(parameters)"          -> true,
    "(binding)"             -> true,
    "(contact)"             -> true,
    "(externalDocs)"        -> true,
    "(license)"             -> true,
    "x-base-uri-parameters" -> true,
    "(base-uri-parameters)" -> true,
    "x-annotationTypes"     -> true,
    "(deprecated)"          -> true,
    "(summary)"             -> true,
    "(externalDocs)"        -> true,
    "x-request-payloads"    -> true,
    "(request-payloads)"    -> true,
    "x-response-payloads"   -> true,
    "x-uses"                -> true,
    "(response-payloads)"   -> true,
    "x-media-type"          -> true,
    "(media-type)"          -> true,
    "(readOnly)"            -> true,
    "(dependencies)"        -> true,
    "(tuple)"               -> true,
    "(format)"              -> true,
    "(exclusiveMaximum)"    -> true,
    "(exclusiveMinimum)"    -> true,
    "x-traits"              -> true,
    "x-resourceTypes"       -> true,
    "x-is"                  -> true,
    "x-type"                -> true,
    "(consumes)"            -> true,
    "(produces)"            -> true,
    "x-fragment-type"       -> true,
    "x-usage"               -> true,
    "x-title"               -> true,
    "x-user-documentation"  -> true,
    "(flow)"                -> true,
    "x-displayName"         -> true,
    "x-describedBy"         -> true,
    "x-requestTokenUri"     -> true,
    "x-authorizationUri"    -> true,
    "x-tokenCredentialsUri" -> true,
    "x-signatures"          -> true,
    "x-settings"            -> true,
    "x-securitySchemes"     -> true
  )

  def normalAnnotation(field: String): Boolean =
    if (isRamlAnnotation(field) || isOasAnnotation(field)) {
      !annotations.getOrElse(field, false)
    } else {
      false
    }

  def isOasAnnotation(field: String): Boolean  = field.startsWith("x-") || field.startsWith("X-")
  def isRamlAnnotation(field: String): Boolean = field.startsWith("(") && field.endsWith(")")

  def parseRamlName(s: String): String = s.replace("(", "").replace(")", "")
  def parseOasName(s: String): String  = s.replace("x-", "").replace("X-", "")

  def parseName(s: String): String = parseOasName(parseRamlName(s))
}
