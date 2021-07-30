package amf.core.internal.plugins.parse

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.config.ParsingOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.exception.UnsupportedParsedDocumentException
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SyamlParsedDocument}
import amf.core.internal.parser.Root
import amf.core.internal.plugins.document.graph.parser.{
  EmbeddedGraphParser,
  FlattenedUnitGraphParser,
  GraphDependenciesReferenceHandler
}
import amf.core.internal.remote.Vendor

object AMFGraphParsePlugin extends AMFParsePlugin {

  override val id: String = Vendor.AMF.name

  override def applies(element: Root): Boolean = element.parsed match {
    case parsed: SyamlParsedDocument =>
      FlattenedUnitGraphParser.canParse(parsed) || EmbeddedGraphParser.canParse(parsed)
    case _ => false
  }

  override def priority: PluginPriority = NormalPriority

  override def parse(document: Root, ctx: ParserContext): BaseUnit = document.parsed match {
    case parsed: SyamlParsedDocument if FlattenedUnitGraphParser.canParse(parsed) =>
      FlattenedUnitGraphParser(ctx.config)
        .parse(parsed.document, effectiveUnitUrl(document.location, ctx.parsingOptions))
    case parsed: SyamlParsedDocument if EmbeddedGraphParser.canParse(parsed) =>
      EmbeddedGraphParser(ctx.config).parse(parsed.document, effectiveUnitUrl(document.location, ctx.parsingOptions))
    case _ => throw UnsupportedParsedDocumentException
  }

  override def mediaTypes: Seq[String] = Seq(
      "application/graph",
      "application/graph+json",
      "application/graph+jsonld",
      "application/amf",
      "application/amf+json",
      "application/amf+jsonld"
  )

  override def validMediaTypesToReference: Seq[String] = Seq.empty

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = GraphDependenciesReferenceHandler

  override def allowRecursiveReferences: Boolean = true

  private def effectiveUnitUrl(location: String, options: ParsingOptions): String = {
    options.definedBaseUrl match {
      case Some(url) => url
      case None      => location
    }
  }
}
