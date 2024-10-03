package amf.core.client.scala.config

/** Immutable implementation of parsing options
  */
case class ParsingOptions(
    amfJsonLdSerialization: Boolean = true,
    baseUnitUrl: Option[String] = None,
    maxYamlReferences: Option[Int] = None,
    maxJSONComplexity: Option[Int] = None,
    maxJsonYamlDepth: Option[Int] = None,
    tokens: Boolean = false,
    extensionsEverywhere: Boolean = false
) {

  /** Parse specific AMF JSON-LD serialization */
  def withoutAmfJsonLdSerialization: ParsingOptions = copy(amfJsonLdSerialization = false)

  /** Parse regular JSON-LD serialization */
  def withAmfJsonLdSerialization: ParsingOptions = copy(amfJsonLdSerialization = true)

  /** Include the BaseUnit Url */
  def withBaseUnitUrl(baseUnit: String): ParsingOptions = copy(baseUnitUrl = Some(baseUnit))

  /** Exclude the BaseUnit Url */
  def withoutBaseUnitUrl: ParsingOptions = copy(baseUnitUrl = None)

  /** Defines an upper bound of yaml alias that will be resolved when parsing a DataNode */
  def setMaxYamlReferences(value: Int): ParsingOptions = copy(maxYamlReferences = Some(value))

  /** Defines the maximum of combining complexity that will be supported when converting a JSON Schema to an AML Dialect
    */
  def setMaxJSONComplexity(value: Int): ParsingOptions = copy(maxJSONComplexity = Some(value))

  /** Defines the maximum amount of nesting depth that a parsed JSON or YAML can have
    */
  def setMaxJsonYamlDepth(value: Int): ParsingOptions = copy(maxJsonYamlDepth = Some(value))

  /** Keep tokens when parsing with SYAML */
  def withTokens: ParsingOptions = copy(tokens = true)

  /** Discard tokens when parsing with SYAML */
  def withoutTokens: ParsingOptions = copy(tokens = false)

  /** Allows extension parsing in every node of the API (valid for OAS 3.0) */
  def withExtensionsEverywhere: ParsingOptions = copy(extensionsEverywhere = true)

  /** Does not allow extension parsing in every node of the API (valid for OAS 3.0) */
  def withoutExtensionsEverywhere: ParsingOptions = copy(extensionsEverywhere = false)

  def isAmfJsonLdSerialization: Boolean = amfJsonLdSerialization
  def definedBaseUrl: Option[String]    = baseUnitUrl
  def getMaxYamlReferences: Option[Int] = maxYamlReferences
  def getMaxJsonYamlDepth: Option[Int]  = maxJsonYamlDepth
  def isTokens: Boolean                 = tokens
  def isExtensionsEverywhere: Boolean   = extensionsEverywhere
}
