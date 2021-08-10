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
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{IllegalTypeHandler, ParseErrorHandler, SyamlException, YError}

class GraphParserContext(rootContextDocument: String = "",
                         refs: Seq[ParsedReference] = Seq.empty,
                         futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
                         config: ParseConfiguration,
                         val graphContext: GraphContext = GraphContext())
    extends SyamlBasedParserErrorHandler(rootContextDocument, refs, futureDeclarations, config) {}
