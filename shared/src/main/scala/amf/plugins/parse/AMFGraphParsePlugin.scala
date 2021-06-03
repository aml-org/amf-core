package amf.plugins.parse

import amf.client.remod.amfcore.config.ParsingOptions
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.exception.UnsupportedParsedDocumentException
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler, SyamlParsedDocument}
import amf.core.rdf.{RdfModelDocument, RdfModelParser}
import amf.core.remote.Vendor
import amf.plugins.document.graph.parser.{
  EmbeddedGraphParser,
  FlattenedUnitGraphParser,
  GraphDependenciesReferenceHandler
}

object AMFGraphParsePlugin extends AMFParsePlugin {

  override val id: String = Vendor.AMF.name

  override def applies(element: Root): Boolean = element.parsed match {
    case parsed: SyamlParsedDocument =>
      FlattenedUnitGraphParser.canParse(parsed) || EmbeddedGraphParser.canParse(parsed)
    case _: RdfModelDocument => true
    case _                   => false
  }

  override def priority: PluginPriority = NormalPriority

  override def parse(document: Root, ctx: ParserContext): BaseUnit = document.parsed match {
    case parsed: SyamlParsedDocument if FlattenedUnitGraphParser.canParse(parsed) =>
      FlattenedUnitGraphParser(ctx.config)
        .parse(parsed.document, effectiveUnitUrl(document.location, ctx.parsingOptions))
    case parsed: SyamlParsedDocument if EmbeddedGraphParser.canParse(parsed) =>
      EmbeddedGraphParser(ctx.config).parse(parsed.document, effectiveUnitUrl(document.location, ctx.parsingOptions))
    case parsed: RdfModelDocument =>
      RdfModelParser(ctx.config).parse(parsed.model, effectiveUnitUrl(document.location, ctx.parsingOptions))
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
