package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.document.{CompilerReferenceCollector, EmptyReferenceCollector, ParsedDocument, ParserContext, ReferenceHandler, SyamlParsedDocument, UnspecifiedReference}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import org.mulesoft.common.functional.MonadInstances._
import org.yaml.model._
import amf.core.internal.parser._
import amf.core.internal.plugins.syntax.{SYamlAMFParserErrorHandler, SyamlAMFErrorHandler}

object GraphDependenciesReferenceHandler extends ReferenceHandler {

  val graphDependenciesPredicate: String = (Namespace.Document + "graphDependencies").iri()

  override def collect(inputParsed: ParsedDocument, ctx: ParserContext): CompilerReferenceCollector = {
    implicit val errorHandler: SyamlAMFErrorHandler = new SyamlAMFErrorHandler(ctx.eh)

    inputParsed match {
      case parsed: SyamlParsedDocument =>
        val document = parsed.document
        document.tagType match {
          case YType.Map =>
            collectFromFlattened(document)
          case YType.Seq =>
            collectFromEmbedded(document)
        }
      case _ => EmptyReferenceCollector
    }
  }

  private def collectFromFlattened(document: YDocument)(implicit errorHandler: IllegalTypeHandler) = {
    val rootNode = FlattenedUnitGraphParser.findRootNode.runCached(document)
    rootNode match {
      case Some(rootNode) if rootNode.tagType == YType.Map =>
        collectGraphDependenciesFrom(rootNode.as[YMap])
      case _ => EmptyReferenceCollector
    }
  }

  private def collectFromEmbedded(document: YDocument)(implicit errorHandler: IllegalTypeHandler) = {
    val maybeMaps = document.node.toOption[Seq[YMap]]
    maybeMaps.flatMap(s => s.headOption).fold[CompilerReferenceCollector](EmptyReferenceCollector) { map =>
      collectGraphDependenciesFrom(map)
    }
  }

  private def collectGraphDependenciesFrom(map: YMap)(implicit errorHandler: IllegalTypeHandler) = {
    map.entries.find(_.key.as[String] == graphDependenciesPredicate) match {
      case Some(entry) => processDependencyEntry(entry)
      case None        => EmptyReferenceCollector
    }
  }

  protected def processDependencyEntry(entry: YMapEntry)(
      implicit errorHandler: IllegalTypeHandler): CompilerReferenceCollector = {
    entry.value.tagType match {
      case YType.Seq =>
        val links: IndexedSeq[(String, YNode)] = collectLinks(entry)
        val collector                          = CompilerReferenceCollector()
        links.foreach {
          case (link, linkEntry) => collector += (link, UnspecifiedReference, linkEntry.location)
        }
        collector
    }
  }

  private def collectLinks(entry: YMapEntry)(implicit errorHandler: IllegalTypeHandler) =
    entry.value.as[YSequence].nodes.flatMap { node =>
      node.tagType match {
        case YType.Map => parseLink(node)
        case _         => None
      }
    }

  private def parseLink(node: YNode)(implicit errorHandler: IllegalTypeHandler): Option[(String, YNode)] = {
    node.as[YMap].entries.find(_.key.as[String] == JsonLdKeywords.Id) match {
      case Some(entry) => Some((entry.value.as[String], entry.value))
      case _           => None
    }
  }
}
