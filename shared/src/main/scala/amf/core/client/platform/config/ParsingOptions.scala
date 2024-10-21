package amf.core.client.platform.config

import amf.core.client.scala.config.{ParsingOptions => InternalParsingOptions}
import amf.core.internal.convert.CoreClientConverters._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class ParsingOptions(private[amf] val _internal: InternalParsingOptions) {

  @JSExportTopLevel("ParsingOptions")
  def this() = this(InternalParsingOptions())

  def isAmfJsonLdSerialization: Boolean       = _internal.amfJsonLdSerialization
  def definedBaseUrl: ClientOption[String]    = _internal.baseUnitUrl.asClient
  def getMaxYamlReferences: ClientOption[Int] = _internal.maxYamlReferences.asClient
  def getMaxJSONComplexity: ClientOption[Int] = _internal.maxJSONComplexity.asClient
  def getMaxJsonYamlDepth: ClientOption[Int]  = _internal.maxJsonYamlDepth.asClient
  def isTokens: Boolean                       = _internal.tokens

  /** Parse specific AMF JSON-LD serialization */
  def withoutAmfJsonLdSerialization(): ParsingOptions = _internal.withoutAmfJsonLdSerialization

  /** Parse regular JSON-LD serialization */
  def withAmfJsonLdSerialization(): ParsingOptions = _internal.withAmfJsonLdSerialization

  /** Include the BaseUnit Url */
  def withBaseUnitUrl(baseUnit: String): ParsingOptions = _internal.withBaseUnitUrl(baseUnit)

  /** Exclude the BaseUnit Url */
  def withoutBaseUnitUrl(): ParsingOptions = _internal.withoutBaseUnitUrl

  /** Defines an upper bound of yaml alias that will be resolved when parsing a DataNode */
  def setMaxYamlReferences(value: Int): ParsingOptions = _internal.setMaxYamlReferences(value)

  /** Defines the maximum of combining complexity that will be supported when converting a JSON Schema to an AML Dialect
    */
  def setMaxJSONComplexity(value: Int): ParsingOptions = _internal.setMaxJSONComplexity(value)

  /** Defines the maximum amount of nesting depth that a parsed JSON or YAML can have
    */
  def setMaxJsonYamlDepth(value: Int): ParsingOptions = _internal.setMaxJsonYamlDepth(value)

  /** Keep tokens when parsing with SYAML */
  def withTokens(): ParsingOptions = _internal.withTokens

  /** Discard tokens when parsing with SYAML */
  def withoutTokens(): ParsingOptions = _internal.withoutTokens
}
