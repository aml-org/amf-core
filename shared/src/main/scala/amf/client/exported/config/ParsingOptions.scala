package amf.client.exported.config

import amf.client.convert.CoreClientConverters._
import amf.client.remod.amfcore.config.{ParsingOptions => InternalParsingOptions}
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("ParsingOptions")
case class ParsingOptions(private[amf] val _internal: InternalParsingOptions) {

  def this() = this(InternalParsingOptions())

  def isAmfJsonLdSerialization: Boolean        = _internal.amfJsonLdSerialization
  def definedBaseUrl: ClientOption[String]     = _internal.baseUnitUrl.asClient
  def getMaxYamlReferences: ClientOption[Long] = _internal.maxYamlReferences.asClient

  /** Parse specific AMF JSON-LD serialization */
  def withoutAmfJsonLdSerialization: ParsingOptions = _internal.withoutAmfJsonLdSerialization

  /** Parse regular JSON-LD serialization */
  def withAmfJsonLdSerialization: ParsingOptions = _internal.withAmfJsonLdSerialization

  /** Include the BaseUnit Url */
  def withBaseUnitUrl(baseUnit: String): ParsingOptions = _internal.withBaseUnitUrl(baseUnit)

  /** Exclude the BaseUnit Url */
  def withoutBaseUnitUrl(): ParsingOptions = _internal.withoutBaseUnitUrl()

  /** Defines an upper bound of yaml alias that will be resolved when parsing a DataNode */
  def setMaxYamlReferences(value: Long): ParsingOptions = _internal.setMaxYamlReferences(value)
}
