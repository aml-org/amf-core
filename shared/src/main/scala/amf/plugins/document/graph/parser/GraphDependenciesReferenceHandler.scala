package amf.plugins.document.graph.parser

import amf.core.parser.errorhandler.ParserErrorHandler
import amf.core.parser.{ParsedDocument, ParserContext, ReferenceHandler, _}
import amf.core.vocabulary.Namespace
import amf.plugins.document.graph.JsonLdKeywords
import org.yaml.model._

object GraphDependenciesReferenceHandler extends ReferenceHandler {

  val graphDependenciesPredicate: String = (Namespace.Document + "graphDependencies").iri()

  override def collect(inputParsed: ParsedDocument, ctx: ParserContext): CompilerReferenceCollector = {
    implicit val errorHandler: ParserErrorHandler = ctx.eh

    inputParsed match {
      case parsed: SyamlParsedDocument =>
        val document  = parsed.document
        val maybeMaps = document.node.toOption[Seq[YMap]]
        maybeMaps.flatMap(s => s.headOption).fold[CompilerReferenceCollector](EmptyReferenceCollector) { map =>
          map.entries.find(_.key.as[String] == graphDependenciesPredicate) match {
            case Some(entry) => processDependencyEntry(entry)
            case None        => EmptyReferenceCollector
          }
        }
      case _ => EmptyReferenceCollector
    }
  }

  protected def processDependencyEntry(entry: YMapEntry)(
      implicit errorHandler: IllegalTypeHandler): CompilerReferenceCollector = {
    entry.value.tagType match {
      case YType.Seq =>
        val links: IndexedSeq[(String, YNode)] = collectLinks(entry)
        val collector                          = CompilerReferenceCollector()
        links.foreach {
          case (link, linkEntry) => collector += (link, UnspecifiedReference, linkEntry)
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
