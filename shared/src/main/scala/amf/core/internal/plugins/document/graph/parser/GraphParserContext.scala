package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.parse.document.{
  EmptyFutureDeclarations,
  ParsedReference,
  ParserContext,
  SyamlBasedParserErrorHandler
}
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.parser.domain.FutureDeclarations
import amf.core.internal.plugins.document.graph.context.GraphContext
import amf.core.internal.plugins.syntax.{SYamlAMFParserErrorHandler, SyamlAMFErrorHandler}
import org.mulesoft.common.client.lexical.SourceLocation
import org.yaml.model.{IllegalTypeHandler, ParseErrorHandler, SyamlException, YError}

class GraphParserContext(
    rootContextDocument: String = "",
    refs: Seq[ParsedReference] = Seq.empty,
    futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
    config: ParseConfiguration,
    val graphContext: GraphContext = GraphContext()
) extends SyamlBasedParserErrorHandler(rootContextDocument, refs, futureDeclarations, config) {

  def addTerms(aliases: Map[String, String]): this.type = {
    aliases.foreach { case (term, id) =>
      graphContext.withTerm(term, id)
    }
    this
  }
}
