package amf.core.internal.plugins.document.graph.parser

import amf.core.internal.parser._
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.plugins.document.graph.context.GraphContext
import amf.core.internal.plugins.syntax.SYamlAMFErrorHandler
import org.yaml.model._

case class JsonLdGraphContextParser(node: YNode, parserContext: GraphParserContext) {
  val context: GraphContext                     = parserContext.graphContext
  implicit val errorHandler: IllegalTypeHandler = new SYamlAMFErrorHandler(parserContext.eh)

  def parse(): GraphContext = {
    node.tagType match {
      case YType.Map => parseMap(node.value.asInstanceOf[YMap])
      case YType.Str => parseRemoteContext()
      case _         =>
        // TODO: throw error
        context
    }
  }

  private def parseMap(map: YMap)(implicit errorHandler: IllegalTypeHandler): GraphContext = {

    map.entries.foreach { entry =>
      val key       = entry.key.as[String]
      val valueType = entry.value.tagType
      (key, valueType) match {
        case (JsonLdKeywords.Base, YType.Str) => context.withBase(entry.value.as[String])
        case (term, YType.Str)                => parseSimpleTermEntry(entry, term)
        case (term, YType.Map)                => parseExpandedTermEntry(entry, term)
        case _                                => // Ignore
      }
    }
    context
  }

  private def parseExpandedTermEntry(entry: YMapEntry, term: String)(implicit errorHandler: IllegalTypeHandler): Unit = {
    val termMap = entry.value.value.asInstanceOf[YMap]

    val id     = termMap.key(JsonLdKeywords.Id).map(_.value.as[String])
    val `type` = termMap.key(JsonLdKeywords.Type).map(_.value.as[String])

    context.withTerm(term, id, `type`)
  }
  private def parseSimpleTermEntry(entry: YMapEntry, term: String)(implicit errorHandler: IllegalTypeHandler): Unit = {
    val namespace = entry.value.as[String]
    context.withTerm(term, namespace)
  }

  private def parseRemoteContext(): GraphContext =
    throw new NotImplementedError("Remote contexts are not supported")
}
